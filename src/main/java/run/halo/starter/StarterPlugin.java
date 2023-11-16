package run.halo.starter;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.service.AttachmentService;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.infra.BackupRootGetter;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import run.halo.starter.model.BackupSetting;

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
        log.debug("插件启动成功start！");
        //注册自定义模型
        schemeManager.register(BackupSetting.class);
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2);
        //自动备份打包
        // ThreadUtil.schedule(scheduledThreadPoolExecutor, () -> {
        //     Backup backup = new Backup();
        //     backup.setApiVersion("migration.halo.run/v1alpha1");
        //     backup.setKind("Backup");
        //     Metadata metadata = new Metadata();
        //     metadata.setGenerateName("backup-");
        //     metadata.setName("");
        //     backup.setMetadata(metadata);
        //     Backup.Spec spec = new Backup.Spec();
        //     //设置有效期
        //     spec.setExpiresAt(TemporalAccessorUtil.toInstant(
        //         LocalDateTimeUtil.offset(LocalDateTime.now(), 1, ChronoUnit.WEEKS)));
        //     backup.setSpec(spec);
        //     reactiveExtensionClient.create(backup).subscribe();
        // }, 0, 60, TimeUnit.SECONDS, true);

    }

    @Override
    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
        //停用时取消注册模型
        Scheme scheme = schemeManager.get(BackupSetting.class);
        schemeManager.unregister(scheme);
        System.out.println("插件停止stop！");
    }

    @Override
    public void delete() {
        System.out.println("插件删除delete！");
    }

}
