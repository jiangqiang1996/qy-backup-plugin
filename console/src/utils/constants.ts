import type {GVK} from "@/utils/request";

const Constants: { [key: string]: GVK } = {
    BackupSettingGvk: {
        group: "www.jiangqiang.top",
        version: "v1",
        kind: "BackupSetting",
        singular: "BackupSetting",
        plural: "BackupSettings",
    }
}

export default Constants;
