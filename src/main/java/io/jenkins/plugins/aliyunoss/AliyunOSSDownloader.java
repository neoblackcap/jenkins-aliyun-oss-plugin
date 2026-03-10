package io.jenkins.plugins.aliyunoss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.aliyunoss.config.AliyunOSSConfig;
import io.jenkins.plugins.aliyunoss.config.AliyunOSSGlobalConfig;
import io.jenkins.plugins.aliyunoss.utils.AliyunOSSClient;
import io.jenkins.plugins.aliyunoss.utils.AliyunOSSException;
import io.jenkins.plugins.aliyunoss.utils.Logger;
import io.jenkins.plugins.aliyunoss.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.ServletException;
import jenkins.MasterToSlaveFileCallable;
import jenkins.tasks.SimpleBuildStep;


import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @date 2024-06-18
 */

public class AliyunOSSDownloader extends Builder implements SimpleBuildStep {
    /**
     * OSS ID
     */
    private String ossId;
    /**
     * The path to download
     */
    private String path;
    /**
     * Download location
     */
    private String location;
    /**
     * Overwrite existing file
     */
    private boolean force = true;
    /**
     * 是否严格模式，文件不存在作业失败
     */
    private boolean strict = true;

    @DataBoundConstructor
    public AliyunOSSDownloader(String ossId, String path) {
        this.ossId = ossId;
        this.path = path;
    }

    @DataBoundSetter
    public void setForce(boolean force) {
        this.force = force;
    }

    @DataBoundSetter
    public void setStrict(boolean strict) {
        this.strict = strict;
    }
    public String getOssId() {
        return ossId;
    }

    public String getPath() {
        return path;
    }

    public String getLocation() {
        return location;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isStrict() {
        return strict;
    }


    @DataBoundSetter
    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Logger logger = new Logger(listener);
        Optional<AliyunOSSConfig> ossConfigOp = AliyunOSSGlobalConfig.getConfig(ossId);
        if (ossConfigOp.isEmpty()) {
            logger.log("Aliyun OSS config not found. please check your config");
            return;
        }
        AliyunOSSConfig ossConfig = ossConfigOp.get();
        FilePath target = workspace;
        String locationEx = env.expand(location);
        if (Utils.isNotEmpty(location)) {
            target = workspace.child(locationEx);
        }
        if (target.exists()) {
            if (force) {
                if (target.isDirectory()) {
                    target.deleteRecursive();
                } else {
                    target.delete();
                }
            } else {
                logger.log("Download failed due to existing target file; set force=true to overwrite target file");
                return;
            }
        }
        String ossPath = Utils.splicePath(ossConfig.getBasePrefix(), env.expand(path));
        target.act(new RemoteDownloader(listener, ossConfig, ossPath, strict, locationEx));
    }

    private static class RemoteDownloader extends MasterToSlaveFileCallable<Void> {

        private static final long serialVersionUID = 1L;

        private final TaskListener taskListener;
        private final AliyunOSSConfig ossConfig;
        private final String path;
        private final boolean strict;
        private final String local;

        private RemoteDownloader(
                TaskListener taskListener, AliyunOSSConfig ossConfig, String path, boolean strict, String local) {
            this.taskListener = taskListener;
            this.ossConfig = ossConfig;
            this.path = path;
            this.strict = strict;
            this.local = local;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            Logger logger = new Logger(taskListener);
            logger.log(
                    "Downloading from aliyun oss. endpoint: %s, bucket: %s, path: %s, localPath: %s",
                    ossConfig.getEndpoint(), ossConfig.getBucket(), path, local);
            OSSClient client;
            try {
                client = AliyunOSSClient.getClient(
                        ossConfig.getEndpoint(),
                        ossConfig.getAccessKey(),
                        ossConfig.getSecretKey(),
                        ossConfig.getBucket());
            } catch (AliyunOSSException e) {
                e.printStackTrace(logger.getStream());
                throw new IOException(e.getMessage());
            }
            if (Objects.isNull(path) || path.endsWith("/")) {
                // 文件夹下所有内容
                ListObjectsV2Result listObjectResult;
                try {
                    ListObjectsV2Request listObjReq = new ListObjectsV2Request(ossConfig.getBucket(), path);
                    listObjReq.setMaxKeys(100);
                    listObjectResult = client.listObjectsV2(listObjReq);
                } catch (OSSException e) {
                    if (strict) {
                        throw e;
                    }
                    logger.log("OSS error code: %s", e.getMessage());
                    return null;
                }
                if (listObjectResult.getKeyCount() <= 0) {
                    if (strict) {
                        throw new IOException("No object found in oss.");
                    } else {
                        logger.log("No object found in oss");
                        return null;
                    }
                }
                if (f.isFile() && listObjectResult.getKeyCount() > 1) {
                    throw new IOException("Workspace location is file but oss path file more then 1");
                }
                for (OSSObjectSummary summary : listObjectResult.getObjectSummaries()) {
                    OSSObject object = client.getObject(ossConfig.getBucket(), summary.getKey());
                    File saveFile;
                    if (Utils.isFile(local)) {
                        saveFile = f;
                    } else {
                        String savePath = Utils.removePrefix(listObjectResult.getPrefix(), summary.getKey());
                        saveFile = new File(f, savePath);
                    }
                    saveFile(saveFile, object);
                    logger.log(
                            "Downloaded file and saved. objectKey: %s, savePath: %s",
                            summary.getKey(), saveFile.getPath());
                }
            } else {
                // 下载文件
                OSSObject object;
                try {
                    object = client.getObject(ossConfig.getBucket(), path);
                } catch (OSSException e) {
                    if (strict) {
                        throw e;
                    }
                    logger.log("OSS error code: %s", e.getMessage());
                    return null;
                }
                File saveFile;
                if (Utils.isFile(local)) {
                    saveFile = f;
                } else {
                    String fileName = Utils.getFileName(object.getKey());
                    saveFile = new File(f, fileName);
                }
                saveFile(saveFile, object);
                logger.log(
                        "Downloaded file and saved. objectKey: %s, savePath: %s", object.getKey(), saveFile.getPath());
            }
            return null;
        }

        private void saveFile(File saveFile, OSSObject object) throws IOException {
            File parent = saveFile.getParentFile();
            if (Objects.nonNull(parent) && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Make dir error");
            }
            FileOutputStream fos = new FileOutputStream(saveFile);
            try (InputStream is = object.getObjectContent()) {
                is.transferTo(fos);
                fos.flush();
                fos.close();
            }
        }
    }

    @Symbol("ossDownload")
    @Extension
    public static class AliyunOSSDownloaderDescriptor extends BuildStepDescriptor<Builder> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Aliyun OSS Downloader";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @POST
        public FormValidation doCheckOssId(@QueryParameter("ossId") String ossId) throws IOException, ServletException {
            if (StringUtils.isBlank(ossId)) {
                return FormValidation.error("Please set OSS config id");
            }
            Optional<AliyunOSSConfig> ossConfigOp = AliyunOSSGlobalConfig.getConfig(ossId);
            if (ossConfigOp.isEmpty()) {
                return FormValidation.error("OSS config id not found");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckPath(@QueryParameter("path") String path) throws IOException, ServletException {
            if (StringUtils.isBlank(path)) {
                return FormValidation.error("Please set OSS path");
            }
            return FormValidation.ok();
        }
    }
}
