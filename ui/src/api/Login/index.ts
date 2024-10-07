const url = '/ssh-script-runner/api/issue-token'

export type LoginRequestBody = {
    user: string
    password: string
}

export default async function Login(
    requestBody: LoginRequestBody
): Promise<void> {
    return fetch(url, {
        headers: {
            'Content-Type': 'application/json',
        },
        method: 'POST',
        body: JSON.stringify(requestBody),
    })
        .then(async (res) => {
            if (res.status === 200) {
                return Promise.resolve()
            } else {
                return Promise.reject(
                    `failed to login (status code: ${res.status}, response: ${await res.text()})`
                )
            }
        })
        .catch((err) => Promise.reject(`failed to login due to ${err}`))
}
