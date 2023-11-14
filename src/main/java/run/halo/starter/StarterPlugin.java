package run.halo.starter;

import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import run.halo.starter.model.BackupSynchronization;

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
        schemeManager.register(BackupSynchronization.class);
        ThreadUtil.sleep(60000);
        UsernamePasswordAuthenticationToken unauthenticated =
            UsernamePasswordAuthenticationToken.unauthenticated("suixin", "suixin");
        ReactiveSecurityContextHolder.withAuthentication(unauthenticated);

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
                LocalDateTimeUtil.offset(LocalDateTime.now(), 1, ChronoUnit.WEEKS)));
            backup.setSpec(spec);
            reactiveExtensionClient.create(backup).subscribe();
        }, 0, 60, TimeUnit.SECONDS, true);
        ThreadUtil.schedule(scheduledThreadPoolExecutor, () -> {

            reactiveExtensionClient.list(Backup.class, backup -> {
                    Backup.Status status = backup.getStatus();
                    return Backup.Phase.SUCCEEDED.equals(status.getPhase())
                        || status.getFilename() != null;
                }, (o1, o2) -> CompareUtil.compare(o1.getMetadata().getCreationTimestamp(),
                    o2.getMetadata().getCreationTimestamp()))
                .flatMap(this::download)
                .flatMap(resource -> {
                    Flux<DataBuffer> dataBuffer = toDataBuffer(resource);
                    return attachmentService.upload("default-policy", null,
                        Objects.requireNonNull(resource.getFilename()),
                        dataBuffer,
                        MediaType.APPLICATION_OCTET_STREAM);
                })
                .subscribe();
        }, 0, 20, TimeUnit.SECONDS, true);
    }

    public Mono<Resource> download(Backup backup) {
        return Mono.create(sink -> {
            var status = backup.getStatus();
            if (!Backup.Phase.SUCCEEDED.equals(status.getPhase()) || status.getFilename() == null) {
                sink.error(new ServerWebInputException("Current backup is not downloadable."));
                return;
            }
            var backupFile = backupRoot.get().resolve(status.getFilename());
            var resource = new FileSystemResource(backupFile);
            if (!resource.exists()) {
                sink.error(
                    new NotFoundException("problemDetail.migration.backup.notFound",
                        new Object[] {},
                        "Backup file doesn't exist or deleted."));
                return;
            }
            sink.success(resource);
        });
    }

    public Flux<DataBuffer> toDataBuffer(Resource resource) {
        DefaultDataBuffer wrap;
        try {
            wrap = DefaultDataBufferFactory.sharedInstance.wrap(resource.getContentAsByteArray());
        } catch (IOException e) {
            return null;
        }
        return Flux.fromIterable(List.of(wrap));
    }

    @Override
    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
        //停用时取消注册模型
        Scheme scheme = schemeManager.get(BackupSynchronization.class);
        schemeManager.unregister(scheme);
        System.out.println("插件停止stop！");
    }

    @Override
    public void delete() {
        System.out.println("插件删除delete！");
    }

}
