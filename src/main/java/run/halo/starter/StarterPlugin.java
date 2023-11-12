package run.halo.starter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.thread.ThreadUtil;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.ExtensionFinder;
import org.pf4j.ExtensionWrapper;
import org.pf4j.PluginRuntimeException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.core.extension.attachment.Policy;
import run.halo.app.core.extension.attachment.endpoint.AttachmentHandler;
import run.halo.app.core.extension.attachment.endpoint.UploadOption;
import run.halo.app.core.extension.service.AttachmentService;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
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

    public StarterPlugin(PluginContext pluginContext,
        ReactiveExtensionClient reactiveExtensionClient, ExtensionClient extensionClient,
        SchemeManager schemeManager, ExternalUrlSupplier externalUrlSupplier,
        AttachmentService attachmentService) {
        super(pluginContext);
        this.reactiveExtensionClient = reactiveExtensionClient;
        this.extensionClient = extensionClient;
        this.schemeManager = schemeManager;
        this.externalUrlSupplier = externalUrlSupplier;
        this.attachmentService = attachmentService;
    }

    @Override
    public void start() {
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        //注册自定义模型
        schemeManager.register(WxConfig.class);
        /**
         * {"apiVersion":"migration.halo.run/v1alpha1","kind":"Backup","metadata":{"generateName
         * ":"backup-","name":""},"spec":{"expiresAt":"2023-11-19T02:07:15.243Z"}}
         */
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


        ThreadUtil.schedule(scheduledThreadPoolExecutor, () -> {
            System.out.println("定时任务");
            upload("default-policy", "", "test", Flux.fromArray(new DataBuffer[0]), MediaType.ALL);
        }, 0, 1, TimeUnit.SECONDS, true);
        System.out.println("插件启动成功start！");
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

    public Mono<Attachment> upload(@NonNull String policyName,
        @Nullable String groupName,
        @NonNull String filename,
        @NonNull Flux<DataBuffer> content,
        @Nullable MediaType mediaType) {
        return authenticationConsumer(
            authentication -> reactiveExtensionClient.get(Policy.class, policyName)
                .flatMap(policy -> {
                    var configMapName = policy.getSpec().getConfigMapName();
                    if (!StringUtils.hasText(configMapName)) {
                        return Mono.error(new ServerWebInputException(
                            "ConfigMap name not found in Policy " + policyName));
                    }
                    return reactiveExtensionClient.get(ConfigMap.class, configMapName)
                        .map(configMap -> UploadOption.from(filename,
                            content,
                            mediaType,
                            policy,
                            configMap));
                })
                .flatMap(uploadContext -> {
                    List<AttachmentHandler> components = new ArrayList<>();
                    components.addAll(SpringUtil.getBeansOfType(AttachmentHandler.class).values());
                    var handlers = List.copyOf(components);
                    return Flux.fromIterable(handlers)
                        .concatMap(handler -> handler.upload(uploadContext))
                        .next();
                })
                .switchIfEmpty(Mono.error(() -> new ServerErrorException(
                    "No suitable handler found for uploading the attachment.", null)))
                .doOnNext(attachment -> {
                    var spec = attachment.getSpec();
                    if (spec == null) {
                        spec = new Attachment.AttachmentSpec();
                        attachment.setSpec(spec);
                    }
                    spec.setOwnerName(authentication.getName());
                    if (StringUtils.hasText(groupName)) {
                        spec.setGroupName(groupName);
                    }
                    spec.setPolicyName(policyName);
                })
                .flatMap(reactiveExtensionClient::create));
    }

    private <T> Mono<T> authenticationConsumer(Function<Authentication, Mono<T>> func) {
        return ReactiveSecurityContextHolder.getContext()
            .switchIfEmpty(Mono.error(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Authentication required.")))
            .map(SecurityContext::getAuthentication)
            .flatMap(func);
    }
}
