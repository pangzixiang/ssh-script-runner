import { Snackbar } from '@mui/material'
import { NotificationState } from './hook.ts'
import { Dispatch } from 'react'

type Props = {
    state: NotificationState
    dispatchNotification: Dispatch<NotificationState>
}

export default function Notification(props: Props) {
    return (
        <>
            <Snackbar
                open={props.state.show}
                autoHideDuration={2000}
                onClose={() =>
                    props.dispatchNotification({
                        show: false,
                        message: '',
                    })
                }
                message={props.state.message}
            />
        </>
    )
}
