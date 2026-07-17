import { createSlice } from "@reduxjs/toolkit";




const authSlice = createSlice({
    name:"authentication",
    initialState:{
        username:null,
        email:null,
        token: localStorage.getItem("JwtToken") || null,
        id: null,
        address:null
    },
    reducers:{
        setAuth: (state, action)=>{
            state.username = action.payload.username
            state.email = action.payload.email
            state.token = action.payload.token
            state.id = action.payload.id
            state.address = action.payload.address
            localStorage.setItem("JwtToken", action.payload.token)
        },
        
        clearAuth:(state, action)=>{
            state.username = null
            state.email = null
            state.token = null
            localStorage.removeItem("JwtToken")
            state.profile = null
            state.address = null
        }
    }
})

export const {setAuth, clearAuth} = authSlice.actions;
export default authSlice.reducer;