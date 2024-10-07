import { NotificationState } from '../Notification/hook.ts'
import { Dispatch, useEffect, useState } from 'react'
import {
    Button,
    FormControlLabel,
    Grid2,
    Stack,
    Switch,
    TextField,
} from '@mui/material'
import SubmitRunProcess, {
    SubmitRunProcessRequest,
} from '../../api/SubmitRunProcess'

type Props = {
    dispatchNotification: Dispatch<NotificationState>
}

export default function RunProcess(props: Props) {
    const [enableJumpServer, setEnableJumpServer] = useState(false)
    const [gitSshUrl, setGitSshUrl] = useState<string>('')
    const [branch, setBranch] = useState<string>('master')
    const [targetHost, setTargetHost] = useState<string>('')
    const [targetUser, setTargetUser] = useState<string>('')
    const [jumpHost, setJumpHost] = useState<string>('')
    const [jumpUser, setJumpUser] = useState<string>('')
    const [consoleLog, setConsoleLog] = useState<string>('')
    const reset = () => {
        setGitSshUrl('')
        setBranch('')
        setTargetHost('')
        setTargetUser('')
        setJumpHost('')
        setJumpUser('')
        setEnableJumpServer(false)
    }
    const submit = () => {
        if (
            gitSshUrl &&
            gitSshUrl !== '' &&
            branch &&
            branch !== '' &&
            targetHost &&
            targetHost !== '' &&
            targetUser &&
            targetUser !== ''
        ) {
            setConsoleLog('')
            let requestBody: SubmitRunProcessRequest = {
                gitSshUrl: gitSshUrl,
                branch: branch,
                targetServer: {
                    host: targetHost,
                    username: targetUser,
                },
            }
            if (jumpHost && jumpHost !== '' && jumpUser && jumpUser !== '') {
                requestBody = {
                    ...requestBody,
                    jumpServer: {
                        host: jumpHost,
                        username: jumpUser,
                    },
                }
            }
            SubmitRunProcess(requestBody)
                .then(() =>
                    props.dispatchNotification({
                        show: true,
                        message: 'Submit successfully',
                    })
                )
                .catch((err) =>
                    props.dispatchNotification({
                        show: true,
                        message: err,
                    })
                )
        } else {
            props.dispatchNotification({
                show: true,
                message: 'Please provide mandatory parameters',
            })
        }
    }
    useEffect(() => {
        const sse = new EventSource('/ssh-script-runner/api/sse-subscription', {
            withCredentials: true,
        })
        sse.addEventListener('logging', (event) => {
            setConsoleLog((prevState) => {
                if (prevState && prevState !== '') {
                    return prevState + '\n' + event.data
                } else {
                    return prevState + event.data
                }
            })
        })
        sse.addEventListener('notification', (event) => {
            props.dispatchNotification({
                show: true,
                message: event.data,
            })
        })

        return () => sse.close()
    }, [props])
    return (
        <Stack spacing={5}>
            <Grid2 spacing={2} container>
                <TextField
                    fullWidth
                    label={'Git ssh url'}
                    value={gitSshUrl}
                    placeholder={'ssh://git@github.com:22/user/repo'}
                    onChange={(event) => setGitSshUrl(event.target.value)}
                    variant="standard"
                />
                <TextField
                    label={'Branch'}
                    value={branch}
                    placeholder={'master'}
                    onChange={(event) => setBranch(event.target.value)}
                    variant="standard"
                />
                <TextField
                    label={'Target host'}
                    value={targetHost}
                    onChange={(event) => setTargetHost(event.target.value)}
                    variant="standard"
                />
                <TextField
                    label={'Target user'}
                    value={targetUser}
                    onChange={(event) => setTargetUser(event.target.value)}
                    variant="standard"
                />
            </Grid2>
            <Grid2 container spacing={2}>
                <FormControlLabel
                    control={
                        <Switch
                            checked={enableJumpServer}
                            onChange={() => {
                                if (!enableJumpServer) {
                                    setJumpUser('')
                                    setJumpHost('')
                                }
                                setEnableJumpServer(!enableJumpServer)
                            }}
                        />
                    }
                    label={`${!enableJumpServer ? 'Disable' : 'Enable'} Jump Server`}
                />
                {enableJumpServer && (
                    <>
                        <TextField
                            label={'Jump host'}
                            value={jumpHost}
                            onChange={(event) =>
                                setJumpHost(event.target.value)
                            }
                            variant="standard"
                        />
                        <TextField
                            label={'Jump user'}
                            value={jumpUser}
                            onChange={(event) =>
                                setJumpUser(event.target.value)
                            }
                            variant="standard"
                        />
                    </>
                )}
            </Grid2>
            <Grid2 container spacing={2}>
                <Button variant="contained" onClick={submit}>
                    Submit
                </Button>
                <Button variant="contained" color="warning" onClick={reset}>
                    Reset
                </Button>
            </Grid2>
            <TextField
                multiline
                value={consoleLog}
                label={'Console'}
                variant="outlined"
                rows={10}
                slotProps={{
                    input: {
                        readOnly: true,
                    },
                }}
            />
        </Stack>
    )
}
