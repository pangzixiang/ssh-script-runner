import { NotificationState } from '../Notification/hook.ts'
import { Dispatch, useCallback, useEffect, useState } from 'react'
import {
    DeleteSshKey,
    GetAllSshKey,
    KeyFile,
    UploadSshKey,
} from '../../api/SshKey'
import {
    Button,
    Checkbox,
    Paper,
    Stack,
    styled,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
} from '@mui/material'
import CloudUploadIcon from '@mui/icons-material/CloudUpload'

type Props = {
    dispatchNotification: Dispatch<NotificationState>
}

const VisuallyHiddenInput = styled('input')({
    clip: 'rect(0 0 0 0)',
    clipPath: 'inset(50%)',
    height: 1,
    overflow: 'hidden',
    position: 'absolute',
    bottom: 0,
    left: 0,
    whiteSpace: 'nowrap',
    width: 1,
})

export default function KeyMaintenance(props: Props) {
    const [keys, setKeys] = useState<KeyFile[]>([])
    const [selectedKey, setSelectedKey] = useState('')
    const initTableData = useCallback(() => {
        GetAllSshKey()
            .then((data) => {
                setKeys(data)
            })
            .catch((err) =>
                props.dispatchNotification({
                    show: true,
                    message: err,
                })
            )
    }, [props])
    useEffect(() => initTableData(), [initTableData])
    return (
        <Stack
            spacing={2}
            sx={{
                justifyContent: 'center',
                alignItems: 'flex-start',
            }}
        >
            <TableContainer component={Paper}>
                <Table stickyHeader>
                    {keys.length === 0 && (
                        <caption>
                            No keys available, please upload ssh key
                        </caption>
                    )}
                    <TableHead>
                        <TableRow>
                            <TableCell></TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Last Modified</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {keys &&
                            keys.map((key) => (
                                <TableRow
                                    key={key.name}
                                    hover
                                    role="checkbox"
                                    onClick={() => {
                                        if (selectedKey === key.name) {
                                            setSelectedKey('')
                                        } else {
                                            setSelectedKey(key.name)
                                        }
                                    }}
                                >
                                    <TableCell padding="checkbox">
                                        <Checkbox
                                            color="primary"
                                            checked={selectedKey === key.name}
                                            inputProps={{
                                                'aria-labelledby': key.name,
                                            }}
                                        />
                                    </TableCell>
                                    <TableCell>{key.name}</TableCell>
                                    <TableCell>{key.lastModified}</TableCell>
                                </TableRow>
                            ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <Button
                variant="contained"
                disabled={!(selectedKey && selectedKey !== '')}
                onClick={() => {
                    if (selectedKey && selectedKey !== '') {
                        DeleteSshKey(selectedKey)
                            .then(() =>
                                props.dispatchNotification({
                                    show: true,
                                    message: 'Deleted ssh key',
                                })
                            )
                            .catch((err) =>
                                props.dispatchNotification({
                                    show: true,
                                    message: err,
                                })
                            )
                            .finally(() => initTableData())
                    }
                }}
            >
                Delete
            </Button>
            <Button
                component="label"
                role={undefined}
                variant="contained"
                tabIndex={-1}
                startIcon={<CloudUploadIcon />}
            >
                Upload Keys
                <VisuallyHiddenInput
                    type="file"
                    onChange={(event) => {
                        if (
                            event.target.files &&
                            event.target.files.length > 0
                        ) {
                            UploadSshKey(event.target.files)
                                .then(() =>
                                    props.dispatchNotification({
                                        show: true,
                                        message: 'Succeeded to upload keys',
                                    })
                                )
                                .catch((err) =>
                                    props.dispatchNotification({
                                        show: true,
                                        message: err,
                                    })
                                )
                                .finally(() => initTableData())
                        }
                    }}
                    multiple
                />
            </Button>
        </Stack>
    )
}
