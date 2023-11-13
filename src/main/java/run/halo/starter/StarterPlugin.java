package run.halo.starter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import run.halo.app.core.extension.attachment.Attachment;
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
    private final HttpClient httpClient = HttpClient.create()
        .followRedirect(true);
    private final WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();

    public Flux<DataBuffer> fetch(URI uri) {
        return webClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .retrieve()
            .bodyToFlux(DataBuffer.class);
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
        System.out.println("插件启动成功start！");
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        //注册自定义模型
        schemeManager.register(WxConfig.class);
        ThreadUtil.schedule(scheduledThreadPoolExecutor, () -> {
            System.out.println("定时任务");
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
            System.out.println("备份成功\n:" + JSONUtil.toJsonPrettyStr(backup));
            ThreadUtil.sleep(10000);
            reactiveExtensionClient.get(Backup.class, backup.getMetadata().getName())
                .flatMap(this::download)
                .flatMap(resource -> {
                    try {
                        URI uri = resource.getURI();
                        System.out.println("上传文件：uri：" + uri.getPath());
                        return attachmentService.upload("default-policy", null,
                            Objects.requireNonNull(resource.getFilename()), fetch(uri),
                            MediaType.APPLICATION_OCTET_STREAM);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).subscribe(new Consumer<Attachment>() {
                    @Override
                    public void accept(Attachment attachment) {
                        System.out.println(JSONUtil.toJsonPrettyStr(attachment));
                    }
                });
        }, 0, 10, TimeUnit.SECONDS, true);
    }

    public Mono<Resource> download(Backup backup) {
        return Mono.create(sink -> {
            System.out.println(backup);
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
            System.out.println("----------------------------------------");
            sink.success(resource);
        });
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
