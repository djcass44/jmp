<template>
    <div class="content">
        <v-progress-circular :size="100" color="accent" indeterminate></v-progress-circular>
    </div>
</template>
<script>
import axios from "axios";
import { storageUser, storageJWT } from "../../var.js";

export default {
    name: 'Token',
    methods: {
        setLoggedIn() {},
        authChanged() {
            this.jumpUser();
        },
        loadFailed() {},
        jumpUser() {
            let url = new URL(window.location.href);
            if(url.searchParams.has("query")) {
                let query = url.searchParams.get("query");
                axios.get(`${process.env.VUE_APP_BASE_URL}/v2/jump/${query}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                    let target = r.data;
                    window.location.replace(target);
                }).catch(function(error) {
                    console.log(error);
                    that.$emit('snackbar', true, `Failed to load target: ${error.response.status}`);
                });
            }
        }
    }
};
</script>
<style scoped>
.content {
    position: absolute;
    left: 50%;
    top: 50%;
    -webkit-transform: translate(-50%, -50%);
    transform: translate(-50%, -50%);
}
</style>
