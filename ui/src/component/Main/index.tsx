import { ReactNode } from 'react'
import { Container } from '@mui/material'

export default function Main({ children }: { children: ReactNode }) {
    return <Container style={{ margin: 20 }}>{children}</Container>
}
