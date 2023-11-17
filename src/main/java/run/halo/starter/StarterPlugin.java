package run.halo.starter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.service.AttachmentService;
import run.halo.app.extension.*;
import run.halo.app.infra.BackupRootGetter;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.migration.Backup;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import run.halo.starter.model.BackupSetting;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * ReactiveExtensionClient reactiveExtensionClient;
 * ExtensionClient extensionClient;
 * SchemeManager schemeManager;
 * ExternalUrlSupplier externalUrlSupplier;
 *
 * @author guqing
 * @since 1.0.0
 */
@Component
@Slf4j
public class StarterPlugin extends BasePlugin {
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private final ReactiveExtensionClient reactiveExtensionClient;
    private final ExtensionClient extensionClient;
    private final SchemeManager schemeManager;
    private final ExternalUrlSupplier externalUrlSupplier;
    private final AttachmentService attachmentService;
    private final BackupRootGetter backupRoot;

    public StarterPlugin(PluginContext pluginContext,
                         ReactiveExtensionClient reactiveExtensionClient, ExtensionClient extensionClient,
                         SchemeManager schemeManager, ExternalUrlSupplier externalUrlSupplier,
                         AttachmentService attachmentService, BackupRootGetter backupRoot) {
        super(pluginContext);
        this.reactiveExtensionClient = reactiveExtensionClient;
        this.extensionClient = extensionClient;
        this.schemeManager = schemeManager;
        this.externalUrlSupplier = externalUrlSupplier;
        this.attachmentService = attachmentService;
        this.backupRoot = backupRoot;
    }

    @Override
    public void start() {
        //注册自定义模型
        schemeManager.register(BackupSetting.class);
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2);
        List<BackupSetting> list = extensionClient.list(BackupSetting.class, backupSetting -> "backupSetting".equals(backupSetting.getMetadata().getName()), Extension::compareTo);
        Optional<BackupSetting> first = list.stream().findFirst();
        BackupSetting backupSetting;
        backupSetting = first.orElseGet(BackupSetting::new);
        //自动备份打包
        ThreadUtil.schedule(scheduledThreadPoolExecutor, () -> {
            Backup backup = new Backup();
            backup.setApiVersion("migration.halo.run/v1alpha1");
            backup.setKind("Backup");
            Metadata metadata = new Metadata();
            metadata.setGenerateName("backup-");
            metadata.setName("");
            backup.setMetadata(metadata);
            Backup.Spec spec = new Backup.Spec();
            //设置有效期
            spec.setExpiresAt(TemporalAccessorUtil.toInstant(
                    LocalDateTimeUtil.offset(LocalDateTime.now(), backupSetting.getEffectiveDuration(), backupSetting.getEffectiveDurationUnit())));
            backup.setSpec(spec);
            reactiveExtensionClient.create(backup).subscribe();
        }, 0, backupSetting.getPeriod(), backupSetting.getTimeUnit(), true);
    }

    @Override
    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
        //停用时取消注册模型
        Scheme scheme = schemeManager.get(BackupSetting.class);
        schemeManager.unregister(scheme);
    }

    @Override
    public void delete() {
    }

}
