const url = '/ssh-script-runner/api/run'

export type SubmitRunProcessRequest = {
    gitSshUrl: string
    branch: string
    mainScript: string
    targetServer: { host: string; username: string }
    jumpServer?: { host: string; username: string }
    createdTime?: string
}

export default async function SubmitRunProcess(
    requestBody: SubmitRunProcessRequest
): Promise<void> {
    return fetch(url, {
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
        method: 'POST',
    })
        .then(async (res) => {
            if (res.status === 202) {
                return Promise.resolve()
            } else {
                return Promise.reject(
                    `failed to submit request (status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) =>
            Promise.reject(`failed to submit request due to ${err}`)
        )
}
