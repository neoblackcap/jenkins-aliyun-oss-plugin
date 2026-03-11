package io.jenkins.plugins.aliyunoss.utils;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.ObjectMetadata;
import hudson.FilePath;
import io.jenkins.plugins.aliyunoss.config.AliyunOSSConfig;
import java.io.InputStream;
import org.apache.commons.lang.StringUtils;

/**
 * @author Bruce.Wu
 * @date 2024-06-18
 */
public class AliyunOSSClient {

    private AliyunOSSClient() {}

    public static OSSClient getClient(String endpoint, String accessKey, String secretKey, String bucket)
            throws AliyunOSSException {
        try {
            CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKey, secretKey);
            OSSClient client = new OSSClient(endpoint, credentialsProvider, null);
            client.listObjects(bucket);
            return client;
        } catch (Exception e) {
            throw new AliyunOSSException("OSS config validate failure：" + e.getMessage());
        }
    }

    public static void validateOSSBucket(final String endpoint, String accessKey, String secretKey, String bucketName)
            throws AliyunOSSException {
        getClient(endpoint, accessKey, secretKey, bucketName);
    }

    public static void upload(
            Logger logger,
            FilePath workspace,
            final AliyunOSSConfig ossConfig,
            String includes,
            String excludes,
            String parentPath)
            throws AliyunOSSException {
        OSSClient client = getClient(
                ossConfig.getEndpoint(), ossConfig.getAccessKey(), ossConfig.getSecretKey(), ossConfig.getBucket());
        logger.log(
                "Uploading files to oss. endpoint: %s , bucket: %s , includes: %s, excludes: %s",
                ossConfig.getEndpoint(), ossConfig.getBucket(), includes, excludes);
        excludes = StringUtils.defaultIfEmpty(excludes, null);
        try {
            FilePath[] filePaths = workspace.list(includes, excludes);
            if (filePaths.length == 0) {
                throw new AliyunOSSException("No file to upload, please check your params");
            }
            for (FilePath filePath : filePaths) {
                try (InputStream is = filePath.read()) {
                    ObjectMetadata meta = new ObjectMetadata();
                    meta.setContentLength(filePath.length());
                    String key = Utils.splicePath(parentPath, filePath.getName());
                    logger.log("Uploading file: %s, ossPath: %s", filePath.getName(), key);

                    if (key.startsWith("/")) {
                        key = StringUtils.removeStart(key, "/");
                        logger.log("Renamed oss object name: %s", key);
                    }

                    client.putObject(ossConfig.getBucket(), key, is, meta);
                    logger.log("Uploaded file: %s, ossPath: %s", filePath.getName(), key);
                }
            }
        } catch (AliyunOSSException e) {
            e.printStackTrace(logger.getStream());
            throw e;
        } catch (Exception e) {
            e.printStackTrace(logger.getStream());
            throw new AliyunOSSException("List files error:" + e.getMessage());
        }
    }
}
