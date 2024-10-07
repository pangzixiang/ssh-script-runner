import {
    Box,
    AppBar as MuAppBar,
    Toolbar,
    Typography,
    IconButton,
    Menu,
    MenuItem,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import { Dispatch, JSX, SetStateAction, useState } from 'react'

type Props = {
    menuSettings: { display: string; component: JSX.Element }[]
    setComponent: Dispatch<SetStateAction<JSX.Element>>
}

export default function AppBar(props: Props) {
    const [openMenu, setOpenMenu] = useState<boolean>(false)
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
    return (
        <Box sx={{ flexGrow: 1 }}>
            <MuAppBar component="nav">
                <Toolbar>
                    <IconButton
                        size="large"
                        edge="start"
                        color="inherit"
                        aria-label="menu"
                        sx={{ mr: 2 }}
                        onClick={(event) => {
                            setOpenMenu(true)
                            setAnchorEl(event.currentTarget)
                        }}
                    >
                        <MenuIcon />
                    </IconButton>
                    <Menu
                        anchorEl={anchorEl}
                        open={openMenu}
                        onClose={() => {
                            setOpenMenu(false)
                            setAnchorEl(null)
                        }}
                        MenuListProps={{
                            'aria-labelledby': 'basic-button',
                        }}
                    >
                        {props.menuSettings.map((setting) => (
                            <MenuItem
                                key={props.menuSettings
                                    .indexOf(setting)
                                    .toString()}
                                onClick={() => {
                                    props.setComponent(setting.component)
                                    setOpenMenu(false)
                                    setAnchorEl(null)
                                }}
                            >
                                {setting.display}
                            </MenuItem>
                        ))}
                    </Menu>
                    <Typography
                        variant="h6"
                        component="div"
                        sx={{ flexGrow: 1 }}
                    >
                        SSH Script Runner
                    </Typography>
                </Toolbar>
            </MuAppBar>
        </Box>
    )
}
