import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import '@fontsource/roboto/300.css'
import '@fontsource/roboto/400.css'
import '@fontsource/roboto/500.css'
import '@fontsource/roboto/700.css'
import { createTheme, CssBaseline, ThemeProvider } from '@mui/material'
import { CookiesProvider } from 'react-cookie'

const darkTheme = createTheme({
    palette: {
        mode: 'dark',
    },
})

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <CookiesProvider>
            <ThemeProvider theme={darkTheme}>
                <CssBaseline />
                <App />
            </ThemeProvider>
        </CookiesProvider>
    </StrictMode>
)
