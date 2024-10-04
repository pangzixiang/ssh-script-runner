import { NotificationState } from '../Notification/hook.ts'
import { Dispatch, useEffect, useState } from 'react'
import {
    Autocomplete,
    Button,
    FormControlLabel,
    Grid2,
    List,
    ListItem,
    ListItemText,
    Stack,
    Switch,
    TextField,
    Typography,
} from '@mui/material'
import SubmitRunProcess, {
    SubmitRunProcessRequest,
} from '../../api/SubmitRunProcess'
import { GetQueueLock, SetQueueLock } from '../../api/QueueLock'
import Console from '../Console'
import RunHistory from '../../api/RunHistory'

type Props = {
    dispatchNotification: Dispatch<NotificationState>
}

const unique = (arr: string[]) => {
    return Array.from(new Set(arr))
}

export default function RunProcess(props: Props) {
    const [enableJumpServer, setEnableJumpServer] = useState(false)
    const [gitSshUrl, setGitSshUrl] = useState<string | null>(null)
    const [branch, setBranch] = useState<string | null>('master')
    const [mainScript, setMainScript] = useState<string | null>('main.sh')
    const [targetHost, setTargetHost] = useState<string | null>(null)
    const [targetUser, setTargetUser] = useState<string | null>(null)
    const [jumpHost, setJumpHost] = useState<string | null>(null)
    const [jumpUser, setJumpUser] = useState<string | null>(null)
    const [consoleLog, setConsoleLog] = useState<string>('')
    const [queue, setQueue] = useState<SubmitRunProcessRequest[]>([])
    const [lock, setLock] = useState(false)
    const [history, setHistory] = useState<SubmitRunProcessRequest[]>([])
    useEffect(() => {
        GetQueueLock()
            .then((res) => setLock(res))
            .catch((err) =>
                props.dispatchNotification({
                    show: true,
                    message: err,
                })
            )
        RunHistory()
            .then((res) => setHistory(res))
            .catch((err) =>
                props.dispatchNotification({
                    show: true,
                    message: err,
                })
            )
    }, [props])
    const reset = () => {
        setGitSshUrl(null)
        setBranch('master')
        setMainScript('main.sh')
        setTargetHost(null)
        setTargetUser(null)
        setJumpHost(null)
        setJumpUser(null)
        setEnableJumpServer(false)
    }
    const submit = () => {
        if (
            gitSshUrl &&
            gitSshUrl.length > 0 &&
            branch &&
            branch.length > 0 &&
            mainScript &&
            mainScript.length > 0 &&
            targetHost &&
            targetHost.length > 0 &&
            targetUser &&
            targetUser.length > 0
        ) {
            let requestBody: SubmitRunProcessRequest = {
                gitSshUrl: gitSshUrl,
                branch: branch,
                mainScript: mainScript,
                targetServer: {
                    host: targetHost,
                    username: targetUser,
                },
            }
            if (
                jumpHost &&
                jumpHost.length > 0 &&
                jumpUser &&
                jumpUser.length > 0
            ) {
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
                .finally(() => {
                    RunHistory()
                        .then((res) => setHistory(res))
                        .catch((err) =>
                            props.dispatchNotification({
                                show: true,
                                message: err,
                            })
                        )
                })
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
        sse.addEventListener('queue', (event) => {
            const data = JSON.parse(event.data) as SubmitRunProcessRequest[]
            setQueue(data)
        })

        return () => sse.close()
    }, [props])
    return (
        <Stack spacing={5}>
            <Grid2 spacing={2} container>
                <Autocomplete
                    onChange={(_, value: string | null) => {
                        setGitSshUrl(value)
                    }}
                    value={gitSshUrl}
                    inputValue={gitSshUrl ?? undefined}
                    fullWidth
                    onInputChange={(_, value: string | null) => {
                        setGitSshUrl(value)
                    }}
                    renderInput={(params) => (
                        <TextField {...params} label={'Git ssh url'} />
                    )}
                    options={unique(history.map((item) => item.gitSshUrl))}
                />
                <Autocomplete
                    onChange={(_, value: string | null) => {
                        setBranch(value)
                    }}
                    value={branch}
                    sx={{ width: 180 }}
                    inputValue={branch ?? undefined}
                    onInputChange={(_, value: string | null) => {
                        setBranch(value)
                    }}
                    renderInput={(params) => (
                        <TextField {...params} label={'Branch'} />
                    )}
                    options={unique(history.map((item) => item.branch))}
                />
                <Autocomplete
                    onChange={(_, value: string | null) => {
                        setMainScript(value)
                    }}
                    value={mainScript}
                    sx={{ width: 180 }}
                    inputValue={mainScript ?? undefined}
                    onInputChange={(_, value: string | null) => {
                        setMainScript(value)
                    }}
                    renderInput={(params) => (
                        <TextField {...params} label={'Main script'} />
                    )}
                    options={unique(history.map((item) => item.mainScript))}
                />
                <Autocomplete
                    onChange={(_, value: string | null) => {
                        setTargetHost(value)
                    }}
                    value={targetHost}
                    sx={{ width: 230 }}
                    inputValue={targetHost ?? undefined}
                    onInputChange={(_, value: string | null) => {
                        setTargetHost(value)
                    }}
                    renderInput={(params) => (
                        <TextField {...params} label={'Target host'} />
                    )}
                    options={unique(
                        history.map((item) => item.targetServer.host)
                    )}
                />
                <Autocomplete
                    onChange={(_, value: string | null) => {
                        setTargetUser(value)
                    }}
                    value={targetUser}
                    sx={{ width: 180 }}
                    inputValue={targetUser ?? undefined}
                    onInputChange={(_, value: string | null) => {
                        setTargetUser(value)
                    }}
                    renderInput={(params) => (
                        <TextField {...params} label={'Target user'} />
                    )}
                    options={unique(
                        history.map((item) => item.targetServer.username)
                    )}
                />
            </Grid2>
            <Grid2 container spacing={2}>
                <FormControlLabel
                    control={
                        <Switch
                            checked={enableJumpServer}
                            onChange={() => {
                                setEnableJumpServer(!enableJumpServer)
                                if (!enableJumpServer) {
                                    setJumpUser(null)
                                    setJumpHost(null)
                                }
                            }}
                        />
                    }
                    label={`${!enableJumpServer ? 'Disable' : 'Enable'} Jump Server`}
                />
                {enableJumpServer && (
                    <>
                        <Autocomplete
                            onChange={(_, value: string | null) => {
                                setJumpHost(value)
                            }}
                            value={jumpHost}
                            sx={{ width: 230 }}
                            inputValue={jumpHost ?? undefined}
                            onInputChange={(_, value: string | null) => {
                                setJumpHost(value)
                            }}
                            renderInput={(params) => (
                                <TextField {...params} label={'Jump host'} />
                            )}
                            options={unique(
                                history
                                    .map((item) =>
                                        item.jumpServer
                                            ? item.jumpServer.host
                                            : null
                                    )
                                    .filter((value) => value !== null)
                            )}
                        />
                        <Autocomplete
                            onChange={(_, value: string | null) => {
                                setJumpUser(value)
                            }}
                            value={jumpUser}
                            sx={{ width: 180 }}
                            inputValue={jumpUser ?? undefined}
                            onInputChange={(_, value: string | null) => {
                                setJumpUser(value)
                            }}
                            renderInput={(params) => (
                                <TextField {...params} label={'Jump user'} />
                            )}
                            options={unique(
                                history
                                    .map((item) =>
                                        item.jumpServer
                                            ? item.jumpServer.username
                                            : null
                                    )
                                    .filter((value) => value !== null)
                            )}
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
            <Grid2 container spacing={1}>
                <Grid2 size={6}>
                    <Console value={consoleLog} maxRows={20} minRows={20} />
                </Grid2>
                <Grid2 size={6}>
                    <Typography
                        sx={{ mt: 4, mb: 2 }}
                        variant="h6"
                        component="div"
                    >
                        Process Queue:
                        <Button
                            onClick={() => {
                                SetQueueLock(!lock)
                                    .then(() => setLock(!lock))
                                    .catch((err) =>
                                        props.dispatchNotification({
                                            show: true,
                                            message: err,
                                        })
                                    )
                            }}
                        >
                            {lock ? 'unlock' : 'lock'}
                        </Button>
                        <List>
                            {queue &&
                                queue.map((item) => (
                                    <ListItem key={queue.indexOf(item)}>
                                        <ListItemText
                                            primary={`${item.gitSshUrl} ${item.branch}`}
                                            secondary={`${item.targetServer.host} ${item.targetServer.username} ${item.createdTime}`}
                                        />
                                    </ListItem>
                                ))}
                        </List>
                    </Typography>
                </Grid2>
            </Grid2>
        </Stack>
    )
}
