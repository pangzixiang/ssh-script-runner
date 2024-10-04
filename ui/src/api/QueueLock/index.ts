import { RestResponse } from '../Common'

const url = '/ssh-script-runner/api/queue-lock'

export async function SetQueueLock(isLock: boolean): Promise<void> {
    return fetch(url + `?isLock=${isLock}`, {
        method: 'PATCH',
    })
        .then(async (res) => {
            if (res.ok) {
                return Promise.resolve()
            } else {
                return Promise.reject(
                    `failed to lock queue, status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) => Promise.reject(`failed to upload key due to ${err}`))
}

export async function GetQueueLock(): Promise<boolean> {
    return fetch(url, {
        method: 'GET',
    })
        .then(async (res) => {
            if (res.ok) {
                const data = (await res.json()) as RestResponse<boolean>
                return Promise.resolve(data.data)
            } else {
                return Promise.reject(
                    `failed to lock queue, status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) => Promise.reject(`failed to upload key due to ${err}`))
}
