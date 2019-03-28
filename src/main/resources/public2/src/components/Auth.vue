<template>
    <!-- <div id="auth-check" class="mdl-grid" v-cloak>
        <div class="mdl-layout-spacer title"></div>
        <span class="text-light" style="height: 32px; line-height: 32px;">{{ username }}</span>
        <div class="mdl-layout-spacer title"></div>
    </div> -->
    <v-layout>
        <v-flex xs12 sm6 offset-sm3>
            <v-alert :value="!api" outline type="error" class="m2-card">Failed to reach Server</v-alert>
            <div v-if="loading === true" class="text-xs-center pa-4">
                <v-progress-circular :size="50" color="accent" indeterminate></v-progress-circular>
            </div>
        </v-flex>
    </v-layout>
</template>

<script>
import axios from "axios";
import { storageUser, storageJWT, storageRequest } from "../var.js";

export default {
    data() {
        return {
            username: '',
            loading: false,
            api: true // Assume API is reachable so we don't get ugly ui
        }
    },
    methods: {
        checkAPI() {
            this.loading = true;
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2/version`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}` }}).then(r => {
                console.log("API is available");
                that.api = true;
                that.loading = false;
            }).catch(err => {
                console.log("Failed to reach API");
                console.log(err);
                that.api = false;
                that.loading = false;
                that.$emit('snackbar', true, "Failed to reach Server");
            });
        },
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
                if(err.response !== undefined && err.response.status === 403) {
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
                else {
                    // User verification failed, probably 403
                    that.username = "Not authenticated";
                    // User verification failed, nuke local storage
                    that.invalidate();
                    that.$emit('toolbarAuthChanged', false);
                }
            });
        },
        invalidate() {
            localStorage.removeItem(storageUser);
            localStorage.removeItem(storageJWT);
            localStorage.removeItem(storageRequest);
            console.log("User data invalidated");
        },
        init() {
            this.checkAPI();
            this.getAuth();
        }
    }
};
</script>
<style scoped>
.m2-card {
    border-radius: 12px;
}
</style>
