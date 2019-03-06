<template>
    <div class="content">
        <div class="mdl-spinner mdl-js-spinner is-active"></div>
    </div>
</template>
<script>
import { storageUser, storageToken } from "../../var.js";

export default {
    name: 'Token',
    methods: {
        setLoggedIn(login) {
            
        }
    },
    created() {
        let url = new URL(window.location.href);
        if(url.searchParams.has("query")) {
            let query = url.searchParams.get("query");
            let redirect = `${process.env.VUE_APP_BASE_URL}/v1/jump/${query}?token=`;
            if(localStorage.getItem(storageToken) !== null)
                redirect += `${localStorage.getItem(storageToken)}&user=${localStorage.getItem(storageUser)}`;
            else
                redirect += "global";
            window.location.replace(redirect);
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
