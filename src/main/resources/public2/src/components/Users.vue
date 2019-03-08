<template>
    <div id="main-list" v-cloak>
        <div class="mdl-grid" v-if="filter !== ''">
            <div class="mdl-layout-spacer title"></div>
            <p>Searching for '{{ filter }}' ({{ filterResults }} results)</p>
            <div class="mdl-layout-spacer title"></div>
        </div>
        <v-layout>
            <v-flex xs12 sm6 offset-sm3>
                <v-subheader inset v-if="filtered.length > 0">Users</v-subheader>
                <v-card v-if="filtered.length > 0">
                    <v-list two-line subheader>
                        <v-list-tile v-for="user in filtered" :key="user.id" avatar @click="">
                            <v-list-tile-avatar :color="user.role === 'ADMIN' ? 'red darken-4' : 'blue darken-4'">
                                <v-icon dark>account_circle</v-icon>
                            </v-list-tile-avatar>
                            <v-list-tile-content>
                                <v-list-tile-title>{{ user.username }}</v-list-tile-title>
                                <v-list-tile-sub-title>{{ capitalize(user.role.toLowerCase()) }}</v-list-tile-sub-title>
                            </v-list-tile-content>
                            <v-list-tile-action>
                                <v-menu bottom left offset-y origin="top right" transition="scale-transition" min-width="150">
                                    <template v-slot:activator="{ on }">
                                        <v-btn ripple icon v-on="on">
                                            <v-icon>more_vert</v-icon>
                                        </v-btn>
                                    </template>
                                    <v-list>
                                        <v-list-tile v-ripple v-if="user.role === 'USER'" @click="makeAdmin(user.id)"><v-list-tile-title>Make admin</v-list-tile-title></v-list-tile>
                                        <v-list-tile v-ripple v-if="user.role === 'ADMIN'" @click="makeUser(user.id)"><v-list-tile-title>Make user</v-list-tile-title></v-list-tile>
                                        <v-list-tile v-ripple @click="remove(user.id)"><v-list-tile-title>Delete</v-list-tile-title></v-list-tile>
                                    </v-list>
                                </v-menu>
                            </v-list-tile-action>
                        </v-list-tile>
                    </v-list>
                </v-card>
                <div v-if="loading === true" class="text-xs-center pa-4">
                    <v-progress-circular :size="100" color="accent" indeterminate></v-progress-circular>
                </div>
                <div v-if="filtered.length === 0 && loading === false">
                    <h1 class="mdl-h1 text-xs-center">204</h1>
                    <h2 class="mdl-h5 text-xs-center" v-if="filtered.length === 0 && items.length > 0">No results.</h2>
                </div>
                <div v-if="systemInfo !== '' && appInfo !== ''">
                    <v-subheader inset>About</v-subheader>
                    <v-expansion-panel>
                        <v-expansion-panel-content>
                          <template v-slot:header>
                            <div>System information</div>
                          </template>
                          <v-card>
                            <v-card-text><code>{{ systemInfo }}</code></v-card-text>
                          </v-card>
                        </v-expansion-panel-content>
                        <v-expansion-panel-content>
                          <template v-slot:header>
                            <div>Application information</div>
                          </template>
                          <v-card>
                            <v-card-text><code>{{ appInfo }}</code></v-card-text>
                          </v-card>
                        </v-expansion-panel-content>
                    </v-expansion-panel>
                </div>
            </v-flex>
        </v-layout>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageJWT } from "../var.js";

export default {
    name: "Users",
    data() {
        return {
            filter: '',
            filterResults: 0,
            items: [],
            filtered: [],
            loading: false,
            systemInfo: '',
            appInfo: ''
        }
    },
    methods: {
        capitalize(s) {
            return s && s[0].toUpperCase() + s.slice(1);
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
            const url = `${process.env.VUE_APP_BASE_URL}/v2/user/rm/${item.id}`;
            let that = this;
            axios.delete(url, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.items.splice(index, 1); // Delete the item, making vue update
                that.filterItems();
                this.$emit('snackbar', true, "Successfully removed user");
            }).catch(e => {
                console.log(e);
                this.$emit('snackbar', true, `Failed to delete: ${e.response.status}`);
            });
        },
        makeAdmin(id) {
            this.setState(id, "ADMIN");
        },
        makeUser(id) {
            this.setState(id, "USER");
        },
        setState(id, role) {
            let index = this.indexFromId(id);
            let item = this.items[index];
            let that = this;
            axios.patch(`${process.env.VUE_APP_BASE_URL}/v2/user/permission`, `{ "id": "${item.id}", "role": "${role}" }`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                console.log(response.status);
                that.$emit('snackbar', true, `${item.username} is now a ${role.toLowerCase()}!`);
                that.loadItems();
            }).catch((err) => {
                console.log(err);
                that.$emit('snackbar', true, `Failed to add ${item.username} as ${role.toLowerCase()}: ${err.response.status}`);
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
                return item.username.match(regex);
            });
            this.filterResults = this.filtered.length;
            setTimeout(function() {
                componentHandler.upgradeDom();
                componentHandler.upgradeAllRegistered();
            }, 0);
        },
        loadItems() {
            let url = `${process.env.VUE_APP_BASE_URL}/v2/users`;
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
                that.loading = false;
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
            }).catch(function(error) {
                console.log(error); // API is probably unreachable
                that.filterItems();
                that.loading = false;
                that.$emit('snackbar', true, `Failed to load users: ${error.response.status}`);
            });
        },
        pushItem(item) {
            // this.items.push(item);
            // this.filterItems();
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
        let that = this;
        axios.get(`${process.env.VUE_APP_BASE_URL}/v2/info/system`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
            that.systemInfo = r.data;
        }).catch(function(err) {
            console.log(err);
            that.$emit('snackbar', true, `Failed to system info: ${error.response.status}`);
        });
        axios.get(`${process.env.VUE_APP_BASE_URL}/v2/info/app`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
            that.appInfo = r.data;
        }).catch(function(err) {
            console.log(err);
            that.$emit('snackbar', true, `Failed to app info: ${error.response.status}`);
        });
    }
};
</script>
