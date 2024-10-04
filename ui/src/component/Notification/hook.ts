import { Dispatch, useReducer } from 'react'

export type NotificationState = {
    show: boolean
    message: string
}

function reducer(
    _state: NotificationState,
    action: NotificationState
): NotificationState {
    return {
        show: action.show,
        message: action.message,
    }
}

export default function useNotification(): [
    NotificationState,
    Dispatch<NotificationState>,
] {
    const [state, dispatch] = useReducer(reducer, { show: false, message: '' })
    return [state, dispatch]
}
