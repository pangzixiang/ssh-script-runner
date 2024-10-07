import './App.css'
import AppBar from './component/AppBar'
import { useEffect, useMemo, useState } from 'react'
import Main from './component/Main'
import LoginForm from './component/LoginForm'
import { useCookies } from 'react-cookie'
import Notification from './component/Notification'
import useNotification from './component/Notification/hook.ts'
import RunProcess from './component/RunProcess'
import KeyMaintenance from './component/KeyMaintenance'
import { Toolbar } from '@mui/material'

function App() {
    const [auth, setAuth] = useState(false)
    const [cookie] = useCookies(['sshsr_token'])
    const [notificationState, dispatchNotification] = useNotification()
    const menuSettings = useMemo(
        () => [
            {
                display: 'Run Script',
                component: (
                    <RunProcess dispatchNotification={dispatchNotification} />
                ),
            },
            {
                display: 'SSH Key Maintenance',
                component: (
                    <KeyMaintenance
                        dispatchNotification={dispatchNotification}
                    />
                ),
            },
        ],
        []
    )
    const [component, setComponent] = useState(menuSettings[0].component)
    useEffect(() => {
        if (cookie && cookie.sshsr_token) {
            setAuth(true)
        } else {
            setAuth(false)
        }
    }, [cookie])
    return (
        <>
            <AppBar menuSettings={menuSettings} setComponent={setComponent} />
            <Toolbar />
            {auth ? (
                <Main>{component}</Main>
            ) : (
                <LoginForm dispatchNotification={dispatchNotification} />
            )}
            <Notification
                state={notificationState}
                dispatchNotification={dispatchNotification}
            />
        </>
    )
}

export default App
