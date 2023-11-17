<script lang="ts" setup>
// core libs
import {onMounted, ref} from "vue";
import {useI18n} from "vue-i18n";
// components
import {VButton} from "@halo-dev/components";
import service from "@/utils/request";
import Constants from "@/utils/constants";

const {t} = useI18n();
const saving = ref(false);
const data = ref()
const formSchema = ref(
    [
        {
            $formkit: 'number',
            name: 'period',
            label: '执行周期',
            help: '定时任务的执行周期',
            number: "integer",
            validation: 'required|number|min:1',
            value: 1,
        },
        {
            $cmp: 'FormKit',
            props: {
                type: 'radio',
                name: 'timeUnit',
                label: '执行周期时间单位',
                options: [
                    {value: "MINUTES", label: '分'},
                    {value: "HOURS", label: '时'},
                    {value: "DAYS", label: '天'},
                ],
                validation: 'required|matches:MINUTES,HOURS,DAYS',
                value: "DAYS",
                validationVisibility: 'dirty'
            }
        },
        {
            $formkit: 'number',
            name: 'effectiveDuration',
            label: '有效时长',
            help: '备份文件的有效时长',
            number: "integer",
            value: 1,
            validation: 'required|number|min:1',
        },
        {
            $cmp: 'FormKit',
            props: {
                type: 'radio',
                name: 'effectiveDurationUnit',
                label: '有效时长时间单位',
                options: [
                    {value: "DAYS", label: '天'},
                    {value: "WEEKS", label: '周'},
                    {value: "MONTHS", label: '月'},
                    {value: "YEARS", label: '年'},
                ],
                validation: 'required|matches:DAYS,WEEKS,MONTHS,YEARS',
                value: "WEEKS",
                validationVisibility: 'dirty'
            }
        }
    ]
)
const mutate = async (data: any) => {
    saving.value = true;
    try {
        const response = await service.saveOrUpdate(Constants.BackupSettingGvk, "backupSetting", {...data})
        const pluginName = "QyBackupPlugin"
        await service.axiosInstance.put(`/apis/api.console.halo.run/v1alpha1/plugins/${pluginName}/reload`);
    } finally {
        saving.value = false;
    }
}
onMounted(async () => {
    let response = await service.list(Constants.BackupSettingGvk, {fieldSelector: "name=backupSetting"});
    let items = response.data.items;
    if (items?.length) {
        data.value = items[0]
    }
});
</script>
<template>
    <Transition mode="out-in" name="fade">
        <div class="bg-white p-4">
            <div>
                <FormKit
                    id="backup-setting"
                    v-model="data"
                    name="backup-setting"
                    :actions="false"
                    :preserve="true"
                    type="form"
                    @submit="mutate"
                    submit-label="Login"
                >
                    <FormKitSchema :schema="formSchema"/>
                </FormKit>
            </div>
            <div v-permission="['system:migrations:manage']" class="pt-5">
                <div class="flex justify-start">
                    <VButton
                        :loading="saving"
                        type="secondary"
                        @click="$formkit.submit('backup-setting')"
                    >
                        {{ $t("core.common.buttons.save") }}
                    </VButton>
                </div>
            </div>
        </div>
    </Transition>
</template>
