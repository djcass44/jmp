<template>
    <div id="auth-check" class="mdl-grid" v-cloak>
        <div class="mdl-layout-spacer title"></div>
        <span class="text-light" style="height: 32px; line-height: 32px;">{{ username }}&nbsp;&bull;&nbsp;{{ version }}</span>
        <div class="mdl-layout-spacer title"></div>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageJWT, storageRequest } from "../var.js";

export default {
    data() {
        return {
            username: '',
            version: ''
        }
    },
    methods: {
        getAuth() {
            let that = this;
            let username = '';
            if(localStorage.getItem(storageUser) !== null) {
                this.username = `Currently authenticated as ${localStorage.getItem(storageUser)}`;
                username = localStorage.getItem(storageUser);
            }
            else {
                this.username = "Not authenticated";
                this.$emit('toolbarAuthChanged', false);
            }
            const url = `${process.env.VUE_APP_BASE_URL}/v2/oauth/valid`;
            axios.get(url, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}` }}).then(r => {
                return axios.get(`${process.env.VUE_APP_BASE_URL}/v2/user`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}` }}).then((r2) => {
                    let role = r2.data;
                    that.$emit('toolbarAuthChanged', true, role === 'ADMIN');
                }).catch((err) => {
                    console.log(err);
                    that.$emit('toolbarAuthChanged', true, false);
                });
            }).catch((err) => {
                console.log(err);
                console.log("User credential verification failed (this is okay if not yet authenticated)");
                if(err.response !== undefined && err.response.status === 400) {
                    console.log("Token is probably expired, lets try to refresh it");
                    return axios.get(`${process.env.VUE_APP_BASE_URL}/v2/oauth/refresh?refresh_token=${localStorage.getItem(storageRequest)}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}` }}).then((r3) => {
                        localStorage.setItem(storageJWT, r3.data.jwt);
                        localStorage.setItem(storageRequest, r3.data.request);
                        console.log("Successfully retrieved new token, will reauthenticate");
                        that.getAuth();
                    }).catch((err2) => {
                        that.username = "Not authenticated";
                        // User verification failed, nuke local storage
                        that.invalidate();
                        that.$emit('toolbarAuthChanged', false);
                        console.log(`Refresh attempt: ${err2}`);
                    });
                }
            });
        },
        invalidate() {
            localStorage.removeItem(storageUser);
            localStorage.removeItem(storageJWT);
            localStorage.removeItem(storageRequest);
        }
    },
    created() {
        this.getAuth();
        let that = this;
        axios.get(`${process.env.VUE_APP_BASE_URL}/v2/version`).then(r => {
            that.version = r.data;
        });
    }
}


</script>
