<template>
    <v-container grid-list-md text-xs-center>
        <v-layout row wrap>
            <v-flex xs12><h1 class="mdl-h1">404</h1></v-flex>
            <v-flex xs12><p class="mdl-h5">That {{ appNoun }} doesn't exist, or you don't have access!</p></v-flex>
            <v-flex xs12 v-cloak v-if="loading === false"><p class="subheading">{{ status }}</p></v-flex>
            <v-flex xs12 v-cloak v-if="loading === false">
                <v-spacer></v-spacer>
                <div v-for="item in items" :key="item.id">
                    <v-hover>
                        <v-chip v-ripple @click="open(item)" color="primary" text-color="white" slot-scope="{ hover }">
                            <v-avatar class="blue darken-4">
                                <strong v-if="item.image == null || item.image === ''">{{ item.name.substring(0, 1).toUpperCase() }}</strong>
                                <v-img v-if="item.image != null && item.image !== ''" :src="item.image" :lazy-src="item.image" v-on:error="item.image = ''" aspect-ratio="1" class="grey darken-2">
                                    <template v-slot:placeholder>
                                        <v-layout fill-height align-center justify-center ma-0>
                                            <v-progress-circular indeterminate color="grey lighten-5"></v-progress-circular>
                                        </v-layout>
                                    </template>
                                </v-img>
                            </v-avatar>
                            <v-slide-x-transition>
                                <strong class="px-1" v-if="!hover">{{ item.name }}</strong>
                            </v-slide-x-transition>
                            <v-expand-x-transition>
                                <strong class="px-1 transition-fast-in-fast-out" v-if="hover">{{ item.location }}</strong>
                            </v-expand-x-transition>
                        </v-chip>
                    </v-hover>
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
            loading: true,
            appNoun: 'Jump'
        }
    },
    methods: {
        setLoggedIn() {},
        authChanged() {},
        loadFailed() {},
        open(item) {
            window.location.replace(`${process.env.VUE_APP_FE_URL}/jmp?query=${item.name}`);
        }
    },
    mounted: function() {
        this.$emit('postInit');
        this.appNoun = process.env.VUE_APP_BRAND_NOUN;
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
                    that.status = `No similar ${process.env.VUE_APP_BRAND_NOUN}s could be found.`;
                else
                    that.status = `Found ${that.items.length} similar ${process.env.VUE_APP_BRAND_NOUN}s...`;
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
