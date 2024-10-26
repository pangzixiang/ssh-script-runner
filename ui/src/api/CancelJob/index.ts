const url = '/ssh-script-runner/api/cancel'

export default async function CancelJob(): Promise<void> {
    return fetch(url, {
        method: 'POST',
    })
        .then(async (res) => {
            if (res.status === 202) {
                return Promise.resolve()
            } else {
                return Promise.reject(
                    `failed to cancel job, status code: ${res.status}, response: ${await res.text()}`
                )
            }
        })
        .catch((err) =>
            Promise.reject(`failed to generate ssh key due to ${err}`)
        )
}
