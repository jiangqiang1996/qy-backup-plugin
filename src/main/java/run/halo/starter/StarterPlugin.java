package run.halo.starter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import run.halo.app.core.extension.service.AttachmentService;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.infra.BackupRootGetter;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.migration.Backup;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import run.halo.starter.model.WxConfig;

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
    private final ReactiveExtensionClient reactiveExtensionClient;
    private final ExtensionClient extensionClient;
    private final SchemeManager schemeManager;
    private final ExternalUrlSupplier externalUrlSupplier;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private final AttachmentService attachmentService;
    private final BackupRootGetter backupRoot;

    public Flux<DataBuffer> toDataBuffer(Resource resource) throws IOException {
        DefaultDataBuffer wrap =
            DefaultDataBufferFactory.sharedInstance.wrap(resource.getContentAsByteArray());
        return Flux.fromIterable(List.of(wrap));
    }

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
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2);
        //注册自定义模型
        schemeManager.register(WxConfig.class);
        ThreadUtil.schedule(scheduledThreadPoolExecutor, () -> {
            log.debug("开始备份");
            Backup backup = new Backup();
            backup.setApiVersion("migration.halo.run/v1alpha1");
            backup.setKind("Backup");
            Metadata metadata = new Metadata();
            metadata.setGenerateName("backup-");
            metadata.setName("");
            backup.setMetadata(metadata);
            Backup.Spec spec = new Backup.Spec();
            spec.setExpiresAt(TemporalAccessorUtil.toInstant(
                LocalDateTimeUtil.offset(LocalDateTime.now(), 1, ChronoUnit.WEEKS)));
            backup.setSpec(spec);
            extensionClient.create(backup);
            while (true) {
                Optional<Backup> fetch =
                    extensionClient.fetch(Backup.class, backup.getMetadata().getName());
                if (fetch.isPresent()) {//存在
                    var status = fetch.get().getStatus();
                    if (Backup.Phase.PENDING.equals(status.getPhase())
                        || Backup.Phase.RUNNING.equals(status.getPhase())) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (Backup.Phase.SUCCEEDED.equals(status.getPhase())
                        && status.getFilename() != null) {
                        var backupFile = backupRoot.get().resolve(status.getFilename());
                        var fileSystemResource = new FileSystemResource(backupFile);
                        if (!fileSystemResource.exists()) {
                            throw new NotFoundException("备份文件找不到");
                        }
                        System.out.println("备份成功，开始上传");
                        try {
                            Flux<DataBuffer> dataBuffer = toDataBuffer(fileSystemResource);
                            attachmentService.upload("default-policy", null,
                                Objects.requireNonNull(fileSystemResource.getFilename()),
                                dataBuffer,
                                MediaType.APPLICATION_OCTET_STREAM).subscribe(
                                attachment -> log.debug("备份完成:{}",
                                    JSONUtil.toJsonStr(attachment)));
                            break;
                        } catch (IOException e) {
                            throw new RuntimeException("备份失败");
                        }
                    } else if (!Backup.Phase.FAILED.equals(status.getPhase())) {
                        break;
                    }
                }
            }
        }, 0, 10, TimeUnit.SECONDS, true);
    }

    @Override
    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
        //停用时取消注册模型
        Scheme scheme = schemeManager.get(WxConfig.class);
        schemeManager.unregister(scheme);
        System.out.println("插件停止stop！");
    }

    @Override
    public void delete() {
        System.out.println("插件删除delete！");
    }

}
