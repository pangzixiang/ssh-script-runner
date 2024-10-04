import { NotificationState } from '../Notification/hook.ts'
import { Dispatch, useCallback, useEffect, useState } from 'react'
import {
    DeleteSshKey,
    GenerateSshKey,
    GetAllSshKey,
    KeyFile,
    UploadSshKey,
    ViewSshKey,
} from '../../api/SshKey'
import {
    Box,
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Modal,
    Paper,
    Stack,
    styled,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
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

const style = {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 400,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
}

export default function KeyMaintenance(props: Props) {
    const [keys, setKeys] = useState<KeyFile[]>([])
    const [selectedKey, setSelectedKey] = useState('')
    const [keyContent, setKeyContent] = useState('')
    const [generated, setGenerated] = useState<boolean>(false)
    const [newKeyName, setNewKeyName] = useState('')
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
            <Modal
                open={keyContent !== ''}
                onClose={() => setKeyContent('')}
                aria-labelledby="child-modal-title"
                aria-describedby="child-modal-description"
            >
                <Box sx={style}>
                    <h2 id="child-modal-title">{selectedKey}</h2>
                    <p id="child-modal-description">
                        <TextField
                            multiline
                            value={keyContent}
                            variant="outlined"
                            rows={10}
                            fullWidth
                            slotProps={{
                                input: {
                                    readOnly: true,
                                },
                            }}
                        />
                    </p>
                    <Button
                        onClick={() =>
                            navigator.clipboard
                                .writeText(keyContent)
                                .then(() =>
                                    props.dispatchNotification({
                                        show: true,
                                        message: `Copied ${selectedKey} content to clipboard`,
                                    })
                                )
                                .catch((err) =>
                                    props.dispatchNotification({
                                        show: true,
                                        message: err,
                                    })
                                )
                        }
                    >
                        Copy
                    </Button>
                    <Button
                        onClick={() => {
                            const file = new File([keyContent], selectedKey)
                            const url = window.URL.createObjectURL(file)
                            const aTag = document.createElement('a')
                            aTag.href = url
                            aTag.download = selectedKey
                            aTag.click()
                            aTag.remove()
                        }}
                    >
                        Download
                    </Button>
                    <Button onClick={() => setKeyContent('')}>Close</Button>
                </Box>
            </Modal>
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
                        ViewSshKey(selectedKey)
                            .then((res) => setKeyContent(res))
                            .catch((err) =>
                                props.dispatchNotification({
                                    show: true,
                                    message: err,
                                })
                            )
                    }
                }}
            >
                View
            </Button>
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
            <Button
                variant="contained"
                disabled={generated}
                onClick={() => setGenerated(true)}
            >
                Generate
            </Button>
            <Dialog
                open={generated}
                onClose={() => {
                    setGenerated(false)
                    setNewKeyName('')
                }}
            >
                <DialogTitle>New Key Pair</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Please enter the key pair name here:
                    </DialogContentText>
                    <TextField
                        value={newKeyName}
                        onChange={(e) => setNewKeyName(e.target.value)}
                        autoFocus
                        required
                        fullWidth
                        variant="standard"
                    />
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => {
                            setGenerated(false)
                            setNewKeyName('')
                        }}
                    >
                        Cancel
                    </Button>
                    <Button
                        onClick={() => {
                            GenerateSshKey(newKeyName)
                                .then(() => {
                                    props.dispatchNotification({
                                        show: true,
                                        message: 'Succeeded to generate key',
                                    })
                                    setGenerated(false)
                                    setNewKeyName('')
                                    initTableData()
                                })
                                .catch((err) => {
                                    props.dispatchNotification({
                                        show: true,
                                        message: err,
                                    })
                                    setNewKeyName('')
                                    setGenerated(false)
                                })
                        }}
                    >
                        Submit
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    )
}
