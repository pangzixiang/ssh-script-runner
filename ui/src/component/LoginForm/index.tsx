import { Box, Button, TextField } from '@mui/material'
import { Dispatch, useState } from 'react'
import Login from '../../api/Login'
import { NotificationState } from '../Notification/hook.ts'

type Props = {
    dispatchNotification: Dispatch<NotificationState>
}

export default function LoginForm(props: Props) {
    const [user, setUser] = useState('')
    const [password, setPassword] = useState('')
    const handleLogin = () => {
        if (user && user !== '' && password && password !== '') {
            Login({
                user: user,
                password: password,
            })
                .then(() => {
                    props.dispatchNotification({
                        show: true,
                        message: 'Login successfully',
                    })
                })
                .catch((err) => {
                    props.dispatchNotification({
                        show: true,
                        message: `failed to login: ${err}`,
                    })
                })
        } else {
            props.dispatchNotification({
                show: true,
                message: 'please provide username or password',
            })
        }
    }
    return (
        <>
            <Box component="div" margin="auto" width="50%">
                <TextField
                    value={user}
                    onChange={(event) => setUser(event.target.value)}
                    label="User"
                    variant="standard"
                    required
                />
                <br />
                <TextField
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    type="password"
                    label="Password"
                    variant="standard"
                    required
                    onKeyDown={(key) => {
                        if (key.key === 'Enter') {
                            handleLogin()
                        }
                    }}
                />
                <br />
                <Button
                    variant="contained"
                    color="primary"
                    style={{ marginTop: 5 }}
                    onClick={handleLogin}
                >
                    Login
                </Button>
            </Box>
        </>
    )
}
