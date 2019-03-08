<template>
    <div id="auth-check" class="mdl-grid" v-cloak>
        <div class="mdl-layout-spacer title"></div>
        <button class="mdl-button mdl-js-button mdl-button--icon" v-on:click="showAuth">
            <i class="material-icons" style="color: #616161;">account_circle</i>
        </button>
        <span class="text-light" style="height: 32px; line-height: 32px;">{{ username }}&nbsp;&bull;&nbsp;{{ version }}</span>
        <div class="mdl-layout-spacer title"></div>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageJWT } from "../var.js";

export default {
    data() {
        return {
            username: '',
            version: ''
        }
    },
    methods: {
        getAuth() {
            let username = '';
            if(localStorage.getItem(storageUser) !== null) {
                this.username = `Currently authenticated as ${localStorage.getItem(storageUser)}`;
                username = localStorage.getItem(storageUser);
            }
            else {
                this.username = "Not authenticated";
                this.$emit('toolbarAuthChanged', false);
            }
            const url = `${process.env.VUE_APP_BASE_URL}/v2/verify/token`;
            axios.get(url, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}` }}).then(r => {
                return axios.get(`${process.env.VUE_APP_BASE_URL}/v2/user`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}` }}).then((r2) => {
                    let role = r2.data;
                    this.$emit('toolbarAuthChanged', true, role === 'ADMIN');
                }).catch((err) => {
                    console.log(err);
                    this.$emit('toolbarAuthChanged', true, false);
                });
            }).catch((err) => {
                console.log(err);
                console.log("User credential verification failed (this is okay if not yet authenticated)");
                this.username = "Not authenticated";
                // User verification failed, nuke local storage
                this.invalidate();
                this.$emit('toolbarAuthChanged', false);
            });
        },
        showAuth() {
            this.$emit('dialog-auth', true);
        },
        invalidate() {
            localStorage.removeItem(storageUser);
            localStorage.removeItem(storageJWT);
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
