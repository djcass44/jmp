<template>
    <div id="main-list" v-cloak>
        <v-layout>
            <v-flex xs12 sm6 offset-sm3>
                <v-alert :value="loggedIn === false && loading === false && showLoginBanner === true" outline type="info" class="m2-card">
                    <v-layout fill-height>
                        <v-flex xs10 class="text-xs-left pa-2">Login or create an account to create &amp; view additional {{ appNoun }}s</v-flex>
                        <v-flex xs2 class="text-xs-right"><v-btn small icon @click="hideLoginBanner"><v-icon small color="info">close</v-icon></v-btn></v-flex>
                    </v-layout>
                </v-alert>
                <v-subheader inset v-if="loading === false">
                    <div v-if="filter !== ''">{{ appNoun }}s ({{ filterResults }} results)</div>
                    <div v-if="filter === ''">{{ appNoun }}s</div>
                    <v-spacer></v-spacer>
                    <v-menu v-if="loading === false && filtered.length > 1" bottom left offset-y origin="top right" transition="scale-transition" min-width="150">
                        <template v-slot:activator="{ on }">
                            <v-btn ripple icon v-on="on">
                                <v-icon color="grey darken-1">sort</v-icon>
                            </v-btn>
                        </template>
                        <v-list>
                            <v-list-tile v-ripple @click="setSort('name')"><v-icon v-if="sort === 'name'">done</v-icon><v-list-tile-title>Name</v-list-tile-title></v-list-tile>
                            <v-list-tile v-ripple @click="setSort('-name')"><v-icon v-if="sort === '-name'">done</v-icon><v-list-tile-title>Name descending</v-list-tile-title></v-list-tile>
                            <v-list-tile v-ripple @click="setSort('-metaCreation')"><v-icon v-if="sort === '-metaCreation'">done</v-icon><v-list-tile-title>Creation</v-list-tile-title></v-list-tile>
                            <v-list-tile v-ripple @click="setSort('-metaUpdate')"><v-icon v-if="sort === '-metaUpdate'">done</v-icon><v-list-tile-title>Last updated</v-list-tile-title></v-list-tile>
                            <v-list-tile v-ripple @click="setSort('-metaUsage')"><v-icon v-if="sort === '-metaUsage'">done</v-icon><v-list-tile-title>Popularity</v-list-tile-title></v-list-tile>
                        </v-list>
                    </v-menu>
                    <v-btn icon @click="showCreateDialog" v-if="loggedIn === true"><v-icon color="grey darken-1">add</v-icon></v-btn>
                </v-subheader>
                <v-card v-if="filtered.length > 0" class="m2-card">
                    <v-list two-line subheader>
                        <v-slide-y-transition class="py-0" group>
                            <v-list-tile v-for="item in filtered" :key="item.id" avatar @click="">
                                <v-list-tile-avatar color="indigo darken-2">
                                    <v-icon v-if="item.image === null || item.image === ''" large dark>{{ avatar(item) }}</v-icon>
                                    <v-img v-if="item.image !== null && item.image !== ''" :src="item.image" :lazy-src="item.image" v-on:error="item.image = ''" aspect-ratio="1" class="grey darken-2">
                                        <template v-slot:placeholder>
                                            <v-layout fill-height align-center justify-center ma-0>
                                                <v-progress-circular indeterminate color="grey lighten-5"></v-progress-circular>
                                            </v-layout>
                                        </template>
                                    </v-img>
                                </v-list-tile-avatar>
                                <v-list-tile-content>
                                    <v-list-tile-title><span v-html="highlightFilter(item.name)">{{ item.name }}</span></v-list-tile-title>
                                    <v-list-tile-sub-title><span v-html="highlight(item.location)">{{ item.location }}</span><span v-if="item.owner !== null">&nbsp;&bull;&nbsp;{{ item.owner }}</span></v-list-tile-sub-title>
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
                        </v-slide-y-transition>
                    </v-list>
                </v-card>
                <div class="text-xs-center py-2"><v-pagination v-if="filterResults > pageSize" v-model="currentPage" :length="pages" circle @input="filterItems"></v-pagination></div>
                <div v-if="loading === true" class="text-xs-center pa-4">
                    <v-progress-circular :size="100" color="accent" indeterminate></v-progress-circular>
                </div>
                <div v-if="(showZero === true || filtered.length === 0) && loading === false">
                    <v-card class="m2-card">
                        <v-card-title primary-title>
                            <v-avatar color="red darken-4" class="ma-4">
                                <v-icon large dark>sentiment_dissatisfied</v-icon>
                            </v-avatar>
                            <div>
                                <h3 class="headline">Nothing could be found.</h3>
                                <div class="subheading" v-if="items.length === 0">Click the user icon to login and start creating {{ appNoun }}s!</div>
                            </div>
                        </v-card-title>
                    </v-card>
                </div>
                <div class="ma-4"></div>
            </v-flex>
        </v-layout>
        <JumpDialog ref="dialogjump"
            @jumpsPushItem="pushItem"
            @jumpsSetItem="setItem"
            @snackbar="snackbar">
        </JumpDialog>
        <GenericDeleteDialog ref="deleteDialog"
            @doRemove="doRemove">
        </GenericDeleteDialog>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageJWT, storageSortMode, flagSeenLoginBanner, BASE_URL } from "../var.js";

import JumpDialog from '../components/dialog/JumpDialog.vue';
import GenericDeleteDialog from "./dialog/GenericDeleteDialog.vue";

export default {
    name: "Jumps",
    components: {
        JumpDialog,
        GenericDeleteDialog
    },
    data() {
        return {
            filter: '',
            filterResults: 0,
            pages: 1,
            currentPage: 1,
            pageSize: 10,
            sort: 'name',
            sorts: [
                'name',
                '-name',
                '-metaCreation',
                '-metaUpdate',
                '-metaUsage'
            ],
            loggedIn: false,
            showZero: false,
            items: [],
            filtered: [],
            loading: true,
            showLoginBanner: true,
            appNoun: 'Jump',
            ws: null
        }
    },
    mounted: function() {
        if(localStorage.getItem(flagSeenLoginBanner) !== null) {
            this.showLoginBanner = false;
        }
        let localSort = localStorage.getItem(storageSortMode);
        if(localSort === 'username') localSort = 'name'; // Account for difference between Jumps/Users
        if(localSort === '-username') localSort = '-name';
        if(localSort !== null && this.sorts.includes(localSort))
            this.sort = localSort;
        else
            this.sort = this.sorts[0];
        this.$emit('postInit');
        this.appNoun = process.env.VUE_APP_BRAND_NOUN;

        let that = this;
        this.ws = new WebSocket(`ws://${process.env.VUE_APP_URL}/ws`);
        this.ws.onmessage = function(event) {
            console.log(`message: ${event.data}`);
            switch(event.data) {
                case 'EVENT_UPDATE':
                    that.loadItems();
                    break;
            }
        }
        this.ws.onclose = function(event) {
            console.log('disconnected');
            that.$emit('snackbar', true, "Lost connection to server", 0);
        }
        // this.ws.onopen = function(event) {
        //     console.log('connected');
        //     that.$emit('snackbar', false);
        // }
    },
    methods: {
        hideLoginBanner() {
            localStorage.setItem(flagSeenLoginBanner, true);
            this.showLoginBanner = false;
        },
        avatar: function(item) {
            if(item.personal === 1) return 'account_circle';
            else if(item.personal === 2) return 'group';
            else return 'public';
        },
        setSort(sort) {
            this.sort = sort;
            localStorage.setItem(storageSortMode, sort);
            this.filterItems();
        },
        snackbar(visible, text) {
            this.$emit('snackbar', visible, text);
        },
        showCreateDialog() {
            this.$refs.dialogjump.setVisible(true, `New ${process.env.VUE_APP_BRAND_NOUN}`, 'Create');
        },
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
            this.showGDD(id);
        },
        showGDD(id) {
            this.$refs.deleteDialog.setVisible(true, id);
        },
        doRemove(id) {
            let index = this.indexFromId(id);
            let item = this.items[index];
            const url = `${BASE_URL}/v1/jumps/rm/${item.id}`;
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
            this.$refs.dialogjump.setVisible(true, `Edit ${process.env.VUE_APP_BRAND_NOUN}`, 'Update', true, item.id, item.name, item.location, index);
        },
        highlight(text) {
            return text.replace(new RegExp("https?:\\/\\/(www\\.)?"), match => {
                if(text.startsWith("https"))
                    return `<span class="text-https">${match}</span>`;
                else
                    return `<span class="text-http">${match}</span>`;
            });
        },
        highlightFilter(text) {
            return text.replace(new RegExp(this.filter), match => {
                return `<span class="text-highlight">${match}</span>`;
            });
        },
        setFilter(query) {
            this.filter = query;
            this.currentPage = 1;
            this.filterItems();
        },
        updatePage() {
            this.pages = Math.max(Math.ceil(this.filterResults / this.pageSize), 1);
            this.filtered = this.filtered.splice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize);
        },
        filterItems() {
            if(this.filter === '')
                this.filtered = this.items;
            let that = this;
            this.filtered = this.items.filter(item => {
                return item.name.toLowerCase().includes(that.filter.toLowerCase()) || item.location.toLowerCase().includes(that.filter.toLowerCase());
            });
            this.filtered.sort(this.dynamicSort(this.sort));
            this.filterResults = this.filtered.length;
            this.updatePage();
        },
        loadItems() {
            let items = this.items;
            let that = this;
            items.length = 0; // Reset in case this is being called later (e.g. from auth)
            this.loading = true;
            axios.get(`${BASE_URL}/v1/jumps`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                that.items = [];
                console.log("Loaded items: " + response.data.length);
                response.data.map(item => {
                    that.items.push(item);
                    // console.log(`${item.name}: ${item.metaCreation}, ${item.metaUpdate}, ${item.metaUsage}`);
                });
                that.filterItems();
                that.checkItemsLength();
                that.loading = false;
            }).catch(function(error) {
                console.log(error); // API is probably unreachable
                that.filterItems();
                that.checkItemsLength();
                that.loading = false;
                that.$emit('snackbar', true, `Failed to load ${process.env.VUE_APP_BRAND_NOUN}s: ${error.response.status}`);
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
        },
        authChanged(login, admin) {
            this.loadItems();
        },
        loadFailed() {
            this.loading = false;
        },
        dynamicSort: function(property) {
            let sortOrder = 1;
            if(property[0] === "-") {
                sortOrder = -1;
                property = property.substr(1);
            }
            return function (a,b) {
                let result = (a[property] < b[property]) ? -1 : (a[property] > b[property]) ? 1 : 0;
                return result * sortOrder;
            }
        }
    },
};
</script>
<style scoped>
.m2-card {
    border-radius: 12px;
}
</style>
