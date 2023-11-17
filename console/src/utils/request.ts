import qs from "qs";
import axios, {type AxiosResponse} from "axios";
import {Toast} from "@halo-dev/components";


const baseURL = import.meta.env.VITE_API_URL;
const axiosInstance = axios.create({
    baseURL,
    timeout: 50000,
    headers: {'Content-Type': 'application/json'},
    paramsSerializer: {
        serialize: function (params: Record<string, any>) {
            return qs.stringify(params, {allowDots: true});
        }
    },
    withCredentials: true,
});

// 非200状态码就弹窗
axiosInstance.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const errorResponse = error.response;
        if (!errorResponse) {
            return Promise.reject(error);
        }
        const {status} = errorResponse;
        if (status !== 200) {
            Toast.error("status: " + status);
        }
        return Promise.reject(error);
    }
);

/**
 * GVK，生成接口路径
 */
export declare type GVK = {
    group: string;
    version: string;
    kind: string;
    plural: string;
    singular: string;
}
/**
 * 每一个实体的metadata字段的类型
 */
declare type MetadataRequest = {
    name: string;
    version?: number;
    annotations?: Map<string, string>
    creationTimestamp?: Date;
    deletionTimestamp?: Date;
    finalizers?: Array<string>;
    generateName?: string;
    labels?: Map<string, string>
};
/**
 * metaData响应
 */
declare type MetadataResponse = MetadataRequest & {
    name: string;
    version: number;
    creationTimestamp: Date;
    deletionTimestamp: Date;
};
declare type BaseExtensionRequest = {
    metadata: MetadataRequest,
    apiVersion: string;
    kind: string;
    [key: string]: any
}
/**
 * 插件实体响应公共字段
 */
declare type BaseExtensionResponse = {
    metadata: MetadataResponse,
    apiVersion: string;
    kind: string;
    [key: string]: any
}
/**
 * 列表查询条件参数
 */
declare type PageQuery = {
    fieldSelector?: string;
    labelSelector?: string;
    page?: string,
    size?: string,
    sort?: string,
}
/**
 * 列表查询结果
 */
declare type PageRes<T extends BaseExtensionResponse> = {
    first: boolean;
    hasNext: boolean;
    hasPrevious: boolean;
    items: Array<T>;
    last: boolean;
    page: number,
    size: number,
    total: number,
    totalPages: number,
}
const service = {
    async create<T extends BaseExtensionResponse>(gvk: GVK, data: BaseExtensionRequest): Promise<AxiosResponse<T>> {
        data.apiVersion = `${gvk.group}/${gvk.version}`;
        data.kind = gvk.kind;
        return axiosInstance.post<any, AxiosResponse<T>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}`, data);
    },
    async get<T extends BaseExtensionResponse>(gvk: GVK, name: string): Promise<AxiosResponse<T>> {
        return axiosInstance.get<any, AxiosResponse<T>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}/${name}`);
    },
    async delete(gvk: GVK, name: string): Promise<AxiosResponse<any>> {
        return axiosInstance.delete<any, AxiosResponse<any>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}/${name}`);
    },
    async update<T extends BaseExtensionResponse>(gvk: GVK, name: string, data: BaseExtensionRequest): Promise<AxiosResponse<T>> {
        data.apiVersion = `${gvk.group}/${gvk.version}`;
        data.kind = gvk.kind;
        if (!data.metadata) {
            console.error(data)
            return Promise.reject("参数无效")
        }
        data.metadata.name = name;
        if (!data.metadata.version) {
            console.error(data)
            return Promise.reject("version无效")
        }
        return axiosInstance.put<any, AxiosResponse<T>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}/${name}`, data);
    },
    async saveOrUpdate<T extends BaseExtensionResponse>(gvk: GVK, name: string, data: BaseExtensionRequest): Promise<AxiosResponse<T>> {
        if (data.metadata) {
            data.metadata.name = name;
        } else {
            data.metadata = {name}
        }
        const response = await service.list(gvk, {fieldSelector: `name=${name}`});
        const items = response.data.items;
        if (items?.length) {
            const oldData = items[0];
            data.metadata.version = oldData.metadata.version;
            return this.update(gvk, name, data);
        } else {
            return this.create(gvk, data)
        }
    },
    async list<T extends BaseExtensionResponse>(gvk: GVK, query: PageQuery): Promise<AxiosResponse<PageRes<T>>> {
        return axiosInstance.get<any, AxiosResponse<PageRes<T>>, any>(`/apis/${gvk.group}/${gvk.version}/${gvk.plural}`, {
            params: query
        });
    },
}

axiosInstance.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";
export default service;
