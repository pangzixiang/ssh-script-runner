import { RestResponse } from '../Common'

const url = '/ssh-script-runner/api/ssh-key'

export type KeyFile = {
    name: string
    lastModified: string
}

export async function GetAllSshKey(): Promise<KeyFile[]> {
    return fetch(url)
        .then(async (res) => {
            if (res.status === 200) {
                const json = (await res.json()) as RestResponse<KeyFile[]>
                return json.data
            } else {
                return Promise.reject(
                    `failed to get ssh key (status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) => Promise.reject(`failed to get ssh key due to ${err}`))
}

export async function GenerateSshKey(name: string): Promise<void> {
    return fetch(url + `/${name}`, {
        method: 'PATCH',
    })
        .then(async (res) => {
            if (res.status === 200) {
                return Promise.resolve()
            } else {
                return Promise.reject(
                    `failed to generate ssh key (status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) =>
            Promise.reject(`failed to generate ssh key due to ${err}`)
        )
}

export async function ViewSshKey(name: string): Promise<string> {
    return fetch(url + `/${name}`)
        .then(async (res) => {
            if (res.status === 200) {
                const json = (await res.json()) as RestResponse<string>
                return json.data
            } else {
                return Promise.reject(
                    `failed to view ssh key (status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) => Promise.reject(`failed to view ssh key due to ${err}`))
}

export async function DeleteSshKey(name: string): Promise<void> {
    if (!name || name.length === 0) {
        return Promise.reject('key file name not provided')
    }
    return fetch(url + `/${name}`, {
        method: 'DELETE',
    })
        .then(async (res) => {
            if (res.status === 200) {
                return Promise.resolve()
            } else {
                return Promise.reject(
                    `failed to delete ssh key (status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) =>
            Promise.reject(`failed to delete ssh key due to ${err}`)
        )
}

export async function UploadSshKey(files: FileList): Promise<void> {
    const formData = new FormData()
    for (const file of files) {
        formData.append('files', file, file.name)
    }
    return fetch(url, {
        method: 'POST',
        body: formData,
    })
        .then(async (res) => {
            if (res.status === 202) {
                return Promise.resolve()
            } else {
                return Promise.reject(
                    `failed to upload key, status code: ${res.status}, response: ${await res.text()}`
                )
            }
        })
        .catch((err) => Promise.reject(`failed to upload key due to ${err}`))
}
