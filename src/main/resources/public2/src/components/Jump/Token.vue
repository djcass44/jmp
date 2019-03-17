<template>
    <div class="content">
        <v-progress-circular :size="100" color="accent" indeterminate v-if="loading === true"></v-progress-circular>
        <v-flex text-xs-center v-if="loading === false">
            <p class="mdl-h5">An error occurred.</p>
            <v-btn flat color="primary" @click="jumpUser">Retry</v-btn>
        </v-flex>
    </div>
</template>
<script>
import axios from "axios";
import { storageUser, storageJWT } from "../../var.js";

export default {
    name: 'Token',
    data() {
        return {
            loading: true
        }
    },
    mounted: function() {
        this.$emit('postInit');
    },
    methods: {
        setLoggedIn() {},
        authChanged() {
            this.jumpUser();
        },
        loadFailed() {},
        jumpUser() {
            this.loading = true;
            let that = this;
            let url = new URL(window.location.href);
            if(url.searchParams.has("query")) {
                let query = url.searchParams.get("query");
                axios.get(`${process.env.VUE_APP_BASE_URL}/v2/jump/${query}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                    let target = r.data;
                    window.location.replace(target);
                }).catch(function(error) {
                    console.log(error);
                    setTimeout(function() {
                        that.loading = false;
                    }, 500);
                    that.$emit('snackbar', true, `Failed to load target!`);
                });
            }
            else {
                setTimeout(function() {
                    that.loading = false;
                }, 500);
                that.$emit('snackbar', true, `You must specify a target!`);
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
.mdl-h1 {
    color: #616161;
    font-size: 148px;
    font-weight: 300;
}
.mdl-h5 {
    color: #757575;
    font-size: 32px;
    font-weight: 400;
}
</style>
