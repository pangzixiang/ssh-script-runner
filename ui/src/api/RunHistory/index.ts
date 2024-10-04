import { SubmitRunProcessRequest } from '../SubmitRunProcess'
import { RestResponse } from '../Common'

const url = '/ssh-script-runner/api/run/history'

export default async function RunHistory(): Promise<SubmitRunProcessRequest[]> {
    return fetch(url)
        .then(async (res) => {
            if (res.ok) {
                return (
                    (await res.json()) as RestResponse<
                        SubmitRunProcessRequest[]
                    >
                ).data
            } else {
                return Promise.reject(
                    `failed to get run history, status code: ${res.status}, response: ${await res.text()}`
                )
            }
        })
        .catch((err) =>
            Promise.reject(`failed to get run history due to ${err}`)
        )
}
