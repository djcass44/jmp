<template>
    <v-container grid-list-md text-xs-center>
        <v-layout row wrap>
            <v-flex xs12><h1 class="mdl-h1">404</h1></v-flex>
            <v-flex xs12><p class="mdl-h5">That jump doesn't exist, or you don't have access!</p></v-flex>
            <v-flex xs12 v-cloak v-if="loading === false"><p>{{ status }}</p></v-flex>
            <v-flex xs12 v-cloak v-if="loading === false">
                <v-spacer></v-spacer>
                <div v-for="item in items" :key="item">
                    <v-chip v-ripple @click="open(item)" color="primary" text-color="white">
                        <v-avatar class="blue darken-4"><strong>{{ item.substring(0, 1).toUpperCase() }}</strong></v-avatar>
                        <strong class="px-1">{{ item }}</strong>
                    </v-chip>
                </div>
                <v-spacer></v-spacer>
            </v-flex>
            <v-flex xs12 v-if="loading === true" class="text-xs-center pa-4">
                <v-progress-circular :size="100" color="accent" indeterminate></v-progress-circular>
            </v-flex>
        </v-layout>
    </v-container>
</template>
<script>
import axios from "axios";
import { storageUser, storageJWT } from "../../var.js";

export default {
    name: 'Similar',
    data() {
        return {
            status: '',
            items: [],
            loading: true
        }
    },
    methods: {
        setLoggedIn() {},
        authChanged() {},
        loadFailed() {},
        open(itemName) {
            window.location.replace(`${process.env.VUE_APP_FE_URL}/jmp?query=${itemName}`)
        }
    },
    mounted: function() {
        this.$emit('postInit');
    },
    created() {
        this.loading = true;
        let url = new URL(window.location.href);
        if(url.searchParams.has("query")) {
            let query = url.searchParams.get("query");
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2/similar/${query}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function (response) {
                console.log(`Loaded ${response.data.length} item(s)`);
                response.data.map(item => {
                    that.items.push(item);
                });
                if(that.items.length === 0)
                    that.status = "No similar jumps could be found.";
                else
                    that.status = `Found ${that.items.length} similar jumps...`;
                that.loading = false;
            }).catch(function (error) {
                console.log(error);
                that.loading = false;
            });
        }
        else {
            this.status = "No query specified!"
            this.loading = false;
        }
    }
}
</script>
<style scoped>
.mdl-chip-padding {
    left: 4px;
    right: 4px;
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
