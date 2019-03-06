<template>
    <div id="main-list">
        <div class="mdl-grid">
            <div class="mdl-layout-spacer"></div>
            <img src="/public/assets/ic_launcher.png">
            <div class="mdl-layout-spacer"></div>
        </div>
        <div class="mdl-grid">
            <div class="mdl-layout-spacer"></div>
            <h2 class="strong-title">That could not be found!</h2>
            <div class="mdl-layout-spacer"></div>
        </div>
        <div class="mdl-grid">
            <div class="mdl-layout-spacer"></div>
            <p v-cloak>{{ status }}</p>
            <div class="mdl-layout-spacer"></div>
        </div>
        <div class="mdl-grid">
            <div class="mdl-layout-spacer"></div>
            <div v-cloak class="mdl-grid">
                <div v-for="item in items" :key="item" class="mdl-cell">
                    <!-- Contact Chip -->
                    <button type="button" class="mdl-chip mdl-chip--contact" v-on:click="open(item)">
                        <span class="mdl-chip__contact mdl-color--blue mdl-color-text--white">{{ item.substring(0, 1).toUpperCase() }}</span>
                        <span class="mdl-chip__text">{{ item }}</span>
                    </button>
                </div>
            </div>
            <div class="mdl-layout-spacer"></div>
        </div>
    </div>
</template>
<script>
import axios from "axios";
import { storageUser, storageToken } from "../../var.js";

export default {
    name: 'Similar',
    data() {
        return {
            status: '',
            items: []
        }
    },
    methods: {
        setLoggedIn() {

        },
        open(itemName) {
            window.location.replace(`${process.env.VUE_APP_FE_URL}/jmp?query=${itemName}`)
        }
    },
    created() {
        let url = new URL(window.location.href);
        if(url.searchParams.has("query")) {
            let query = url.searchParams.get("query");
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2/similar/${query}`, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(function (response) {
                console.log(`Loaded ${response.data.length} item(s)`);
                response.data.map(item => {
                    that.items.push(item);
                });
                if(that.items.length === 0)
                    that.status = "No similar jumps could be found.";
                else
                    that.status = `Found ${that.items.length} similar jumps...`;
            }).catch(function (error) {
                console.log(error);
            });
        }
        else
            this.status = "No query specified!"
    }
}
</script>
<style scoped>

</style>
