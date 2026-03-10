package io.jenkins.plugins.aliyunoss.config;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;


@Extension
public class AliyunOSSGlobalConfig extends Descriptor<AliyunOSSGlobalConfig>
        implements Describable<AliyunOSSGlobalConfig>, Serializable {

    private static final long serialVersionUID = 1L;

    private List<AliyunOSSConfig> configs = new ArrayList<>();

    public AliyunOSSGlobalConfig(List<AliyunOSSConfig> configs) {
        this.configs = configs;
    }

    public AliyunOSSGlobalConfig() {
        super(AliyunOSSGlobalConfig.class);
        load();
    }

    @Override
    public Descriptor<AliyunOSSGlobalConfig> getDescriptor() {
        return Jenkins.get().getDescriptorByType(AliyunOSSGlobalConfig.class);
    }


    public List<AliyunOSSConfig> getConfigs() {
        return configs;
    }
    public void setConfigs(List<AliyunOSSConfig> configs) {
        this.configs = configs;
    }


    public Descriptor<AliyunOSSConfig> getAliyunOSSConfigDescriptor() {
        return Jenkins.get().getDescriptorByType(AliyunOSSConfig.AliyunOSSConfigDescriptor.class);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }

    public static AliyunOSSGlobalConfig getInstance() {
        return Jenkins.get().getDescriptorByType(AliyunOSSGlobalConfig.class);
    }

    public static Optional<AliyunOSSConfig> getConfig(String id) {
        return getInstance().configs.stream()
                .filter(item -> id.equals(item.getId()))
                .findAny();
    }
}
