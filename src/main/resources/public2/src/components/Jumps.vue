<template>
    <div id="main-list" v-cloak>
        <div class="mdl-grid" v-if="filter !== ''">
            <div class="mdl-layout-spacer title"></div>
            <p>Searching for '{{ filter }}' ({{ filterResults }} results)</p>
            <div class="mdl-layout-spacer title"></div>
        </div>
        <v-layout>
            <v-flex xs12 sm6 offset-sm3>
                <v-subheader inset v-if="filtered.length > 0">Jumps</v-subheader>
                <v-card v-if="filtered.length > 0">
                    <v-list two-line subheader>
                        <v-list-tile v-for="item in filtered" :key="item.id" avatar @click="">
                            <v-list-tile-avatar color="indigo darken-2">
                                <div v-if="item.image == null || item.image === ''">
                                    <v-icon dark v-if="item.personal === false">public</v-icon>
                                    <v-icon dark v-if="item.personal === true">account_circle</v-icon>
                                </div>
                                <v-img v-if="item.image != null && item.image !== ''" :src="item.image" :lazy-src="item.image" aspect-ratio="1" class="grey darken-2">
                                    <template v-slot:placeholder>
                                        <v-layout fill-height align-center justify-center ma-0>
                                            <v-progress-circular indeterminate color="grey lighten-5"></v-progress-circular>
                                        </v-layout>
                                    </template>
                                </v-img>
                            </v-list-tile-avatar>
                            <v-list-tile-content>
                                <v-list-tile-title>{{ item.name }}</v-list-tile-title>
                                <v-list-tile-sub-title><span v-html="highlight(item.location)">{{ item.location }}</span></v-list-tile-sub-title>
                            </v-list-tile-content>
                            <v-list-tile-action>
                                <v-menu bottom left offset-y origin="top right" transition="scale-transition" min-width="150">
                                    <template v-slot:activator="{ on }">
                                        <v-btn ripple icon v-on="on">
                                            <v-icon>more_vert</v-icon>
                                        </v-btn>
                                    </template>
                                    <v-list>
                                        <v-list-tile v-ripple v-clipboard:copy="item.location" v-clipboard:success="copySuccess" v-clipboard:error="copyFailed" @click="">
                                            <v-list-tile-title>Copy URL</v-list-tile-title>
                                        </v-list-tile>
                                        <v-list-tile v-ripple @click="edit(item.id)"><v-list-tile-title>Edit</v-list-tile-title></v-list-tile>
                                        <v-list-tile v-ripple @click="remove(item.id)"><v-list-tile-title>Delete</v-list-tile-title></v-list-tile>
                                    </v-list>
                                </v-menu>
                            </v-list-tile-action>
                        </v-list-tile>
                    </v-list>
                </v-card>
                <div v-if="loading === true" class="text-xs-center pa-4">
                    <v-progress-circular :size="100" color="accent" indeterminate></v-progress-circular>
                </div>
                <div v-if="(showZero === true || filtered.length === 0) && loading === false">
                    <h1 class="mdl-h1 text-xs-center">204</h1>
                    <h2 class="mdl-h5 text-xs-center" v-if="filtered.length === 0 && items.length === 0">No jumps have been created yet.</h2>
                    <h2 class="mdl-h5 text-xs-center" v-if="filtered.length === 0 && items.length > 0">No results.</h2>
                </div>
            </v-flex>
        </v-layout>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageJWT } from "../var.js";

export default {
    name: "Jumps",
    data() {
        return {
            filter: '',
            filterResults: 0,
            loggedIn: false,
            showZero: false,
            items: [],
            filtered: [],
            loading: false
        }
    },
    methods: {
        copyFailed() {
            this.$emit('snackbar', true, "Failed to copy!")
        },
        copySuccess() {
            this.$emit('snackbar', true, "Copied URL to clipboard!")
        },
        checkItemsLength() {
            this.showZero = this.filtered.length === 0;
        },
        indexFromId(id) {
            for(let i = 0; i < this.items.length; i++) {
                if(this.items[i].id === id)
                    return i;
            }
            return -1;
        },
        remove(id) {
            let index = this.indexFromId(id);
            let item = this.items[index];
            this.$emit('dialog-delete', true, item.name, index);
        },
        doRemove(index) {
            let item = this.items[index];
            const url = `${process.env.VUE_APP_BASE_URL}/v1/jumps/rm/${item.id}`;
            let that = this;
            axios.delete(url, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.items.splice(index, 1); // Delete the item, making vue update
                that.filterItems();
                that.checkItemsLength();
            }).catch(e => {
                console.log(e);
                that.$emit('snackbar', true, `Failed to delete: ${e.response.status}`);
            });
        },
        edit(id) {
            let index = this.indexFromId(id);
            let item = this.items[index];
            this.$emit('dialog-create', true, 'Edit jump point', 'Update', true, item.id, item.name, item.location, index);
        },
        highlight(text) {
            return text.replace(new RegExp("https?:\\/\\/(www\\.)?"), match => {
                if(text.startsWith("https"))
                    return `<span class="text-https">${match}</span>`;
                else
                    return `<span class="text-http">${match}</span>`;
            });
        },
        setFilter(query) {
            this.filter = query;
            this.filterItems();
        },
        filterItems() {
            if(this.filter === '')
                this.filtered = this.items;
            let that = this;
            this.filtered = this.items.filter(function(item) {
                let regex = new RegExp(`(${that.filter})`, 'i');
                return item.name.match(regex) || item.location.match(regex);
            });
            this.filterResults = this.filtered.length;
            setTimeout(function() {
                componentHandler.upgradeDom();
                componentHandler.upgradeAllRegistered();
            }, 0);
        },
        loadItems() {
            let url = `${process.env.VUE_APP_BASE_URL}/v1/jumps`;
            let items = this.items;
            let that = this;
            items.length = 0; // Reset in case this is being called later (e.g. from auth)
            this.loading = true;
            axios.get(url, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                console.log("Loaded items: " + response.data.length);
                response.data.map(item => {
                    items.push(item);
                });
                that.filterItems();
                that.checkItemsLength();
                that.loading = false;
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
            }).catch(function(error) {
                console.log(error); // API is probably unreachable
                that.filterItems();
                that.checkItemsLength();
                that.loading = false;
                that.$emit('snackbar', true, `Failed to load jumps: ${error.response.status}`);
            });
        },
        pushItem(item) {
            // this.items.push(item);
            // this.filterItems();
            // this.checkItemsLength();
            this.loadItems();
        },
        setItem(item, index) {
            // this.$set(this.items, index, item);
            // TODO dont reload everything on edit
            this.loadItems();
        },
        setLoggedIn(loggedIn) {
            this.loggedIn = loggedIn;
        }
    },
    created() {
        this.loadItems();
    }
};
</script>
