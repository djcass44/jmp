<template>
    <div id="main-list" v-cloak>
        <v-layout>
            <v-flex xs12 sm6 offset-sm3>
                <p class="subheading ml-3"><v-btn flat icon color="grey darken-1" @click="openHome"><v-icon>arrow_back</v-icon></v-btn>Back to home</p>
                <v-alert :value="login === false && loading === false" outline type="info" class="m2-card">Login or create an account to see users &amp; groups.</v-alert>
                <div v-if="loading === true" class="text-xs-center pa-4">
                    <v-progress-circular :size="100" color="accent" indeterminate></v-progress-circular>
                </div>
                <v-subheader inset v-if="loading === false">
                    <div v-if="filter !== ''">Users ({{ filterResults}} results)</div>
                    <div v-if="filter === ''">Users</div>
                    <v-spacer></v-spacer>
                    <v-menu bottom left offset-y origin="top right" transition="scale-transition" min-width="150" v-if="filtered.length > 1">
                        <template v-slot:activator="{ on }">
                            <v-btn ripple icon v-on="on">
                                <v-icon color="grey darken-1">sort</v-icon>
                            </v-btn>
                        </template>
                        <v-list>
                            <v-list-tile v-ripple @click="setSort('username')"><v-icon v-if="sort === 'username'">done</v-icon><v-list-tile-title>Name</v-list-tile-title></v-list-tile>
                            <v-list-tile v-ripple @click="setSort('-username')"><v-icon v-if="sort === '-username'">done</v-icon><v-list-tile-title>Name descending</v-list-tile-title></v-list-tile>
                            <v-list-tile v-ripple @click="setSort('-metaCreation')"><v-icon v-if="sort === '-metaCreation'">done</v-icon><v-list-tile-title>Creation</v-list-tile-title></v-list-tile>
                            <v-list-tile v-ripple @click="setSort('-metaUpdate')"><v-icon v-if="sort === '-metaUpdate'">done</v-icon><v-list-tile-title>Last modified</v-list-tile-title></v-list-tile>
                        </v-list>
                    </v-menu>
                    <v-btn icon @click="showCreateDialog" v-if="login === true && allowUserCreation === true"><v-icon color="grey darken-1">add</v-icon></v-btn>
                </v-subheader>
                <v-card v-if="filtered.length > 0" class="m2-card">
                    <v-list two-line subheader>
                        <v-slide-y-transition class="py-0" group>
                            <v-list-tile v-for="user in filtered" :key="user.id" avatar @click="">
                                <v-list-tile-avatar :color="user.role === 'ADMIN' ? 'red darken-4' : 'blue darken-4'">
                                    <v-icon large dark v-if="user.role === 'USER'">account_circle</v-icon>
                                    <v-icon large dark v-if="user.role === 'ADMIN'">supervised_user_circle</v-icon>
                                </v-list-tile-avatar>
                                <v-list-tile-content>
                                    <v-list-tile-title><span v-html="highlightFilter(user.username)">{{ user.username }}</span></v-list-tile-title>
                                    <v-list-tile-sub-title>{{ capitalize(user.role.toLowerCase()) }}</v-list-tile-sub-title>
                                </v-list-tile-content>
                                <v-list-tile-action v-if="user.username !== 'admin'">
                                    <v-menu bottom left offset-y origin="top right" transition="scale-transition" min-width="150">
                                        <template v-slot:activator="{ on }">
                                            <v-btn ripple icon v-on="on">
                                                <v-icon>more_vert</v-icon>
                                            </v-btn>
                                        </template>
                                        <v-list>
                                            <v-list-tile v-ripple v-if="user.role === 'USER' && isAdmin === true" @click="makeAdmin(user.id)"><v-list-tile-title>Promote to admin</v-list-tile-title></v-list-tile>
                                            <v-list-tile v-ripple v-if="user.role === 'ADMIN' && isAdmin === true" @click="makeUser(user.id)"><v-list-tile-title>Demote to user</v-list-tile-title></v-list-tile>
                                            <v-list-tile v-ripple @click="showGSD(user.id)"><v-list-tile-title>Set groups</v-list-tile-title></v-list-tile>
                                            <v-list-tile v-ripple v-if="isAdmin === true"@click="remove(user.id)"><v-list-tile-title>Delete</v-list-tile-title></v-list-tile>
                                        </v-list>
                                    </v-menu>
                                </v-list-tile-action>
                            </v-list-tile>
                        </v-slide-y-transition>
                    </v-list>
                </v-card>
                <div v-if="filtered.length === 0 && loading === false">
                    <v-card class="m2-card">
                        <v-card-title primary-title>
                            <v-avatar color="red darken-4" class="ma-4">
                                <v-icon dark>person</v-icon>
                            </v-avatar>
                            <div>
                                <h3 class="display-3 font-weight-light">204</h3>
                                <div class="subheading">No users found.</div>
                            </div>
                        </v-card-title>
                    </v-card>
                </div>
                <v-subheader inset v-if="loading === false">
                    <div v-if="filter !== ''">Groups ({{ groupResults }} results)</div>
                    <div v-if="filter === ''">Groups</div>
                    <v-spacer></v-spacer>
                    <v-btn icon @click="showGCD(true)" v-if="login === true"><v-icon color="grey darken-1">add</v-icon></v-btn>
                </v-subheader>
                <v-card v-if="filteredGroups.length > 0" class="m2-card">
                    <v-list two-line subheader>
                        <v-slide-y-transition class="py-0" group>
                            <v-list-tile v-for="group in filteredGroups" :key="group.id" avatar @click="">
                                <v-list-tile-avatar color="blue darken-4">
                                    <v-icon large dark>domain</v-icon>
                                </v-list-tile-avatar>
                                <v-list-tile-content>
                                    <v-list-tile-title><span v-html="highlightFilter(group.name)">{{ group.name }}</span></v-list-tile-title>
                                </v-list-tile-content>
                                <v-list-tile-action>
                                    <v-menu bottom left offset-y origin="top right" transition="scale-transition" min-width="150">
                                        <template v-slot:activator="{ on }">
                                            <v-btn ripple icon v-on="on">
                                                <v-icon>more_vert</v-icon>
                                            </v-btn>
                                        </template>
                                        <v-list>
                                            <v-list-tile v-ripple @click="showGCD(false, group)"><v-list-tile-title>Edit</v-list-tile-title></v-list-tile>
                                            <v-list-tile v-ripple @click="showGDD(group.id)"><v-list-tile-title>Delete</v-list-tile-title></v-list-tile>
                                        </v-list>
                                    </v-menu>
                                </v-list-tile-action>
                            </v-list-tile>
                        </v-slide-y-transition>
                    </v-list>
                </v-card>
                <div v-if="filteredGroups.length === 0 && loading === false">
                    <v-card class="m2-card">
                        <v-card-title primary-title>
                            <v-avatar color="red darken-4" class="ma-4">
                                <v-icon dark>group</v-icon>
                            </v-avatar>
                            <div>
                                <h3 class="display-3 font-weight-light">204</h3>
                                <div class="subheading">No groups found.</div>
                            </div>
                        </v-card-title>
                    </v-card>
                </div>
                <LDAP ref="ldap"></LDAP>
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
        <GroupDialog ref="groupDialog"
            @snackbar="snackbar"
            @pushItem="loadGroups">
        </GroupDialog>
        <GenericDeleteDialog ref="deleteDialog"
            @doRemove="removeGroup">
        </GenericDeleteDialog>
        <GenericDeleteDialog ref="dialogrmuser"
            @doRemove="doRemove">
        </GenericDeleteDialog>
        <GroupSelectDialog ref="groupSelectDialog"
            @snackbar="snackbar">
        </GroupSelectDialog>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageJWT, storageSortMode } from "../var.js";

import GroupDialog from "./dialog/GroupDialog.vue";
import GenericDeleteDialog from "./dialog/GenericDeleteDialog.vue";
import GroupSelectDialog from "./dialog/GroupSelectDialog.vue";

import LDAP from "./prop/LDAP.vue";

export default {
    name: "Users",
    components: {
        GroupDialog,
        GenericDeleteDialog,
        GroupSelectDialog,
        LDAP
    },
    data() {
        return {
            filter: '',
            filterResults: 0,
            items: [],
            filtered: [],
            sort: 'username',
            sorts: [
                'username',
                '-username',
                '-metaCreation',
                '-metaUpdate',
                '-metaUsage'
            ],
            loading: true,
            systemInfo: '',
            appInfo: '',
            groups: [],
            filteredGroups: [],
            groupResults: 0,
            isAdmin: false,
            login: false,
            allowUserCreation: true
        }
    },
    mounted: function() {
        let localSort = localStorage.getItem(storageSortMode);
        if(localSort === 'name') localSort = 'username'; // Account for difference between Jumps/Users
        if(localSort === '-name') localSort = '-username';
        if(localSort !== null && this.sorts.includes(localSort))
            this.sort = localSort;
        else
            this.sort = this.sorts[0];
        this.$emit('postInit');
    },
    methods: {
        setSort(sort) {
            this.sort = sort;
            localStorage.setItem(storageSortMode, sort);
            this.filterItems();
        },
        showCreateDialog() {
            this.$emit('dialog-create', true);
        },
        showGCD(create, item) {
            this.$refs.groupDialog.setVisible(true, create, item);
        },
        showGDD(id) {
            this.$refs.deleteDialog.setVisible(true, id);
        },
        showGSD(uid) {
            this.$refs.groupSelectDialog.setVisible(true, uid);
        },
        snackbar(visible, text) {
            this.$emit('snackbar', visible, text);
        },
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
        indexFromGId(id) {
            for(let i = 0; i < this.groups.length; i++) {
                if(this.groups[i].id === id)
                    return i;
            }
            return -1;
        },
        remove(id) {
            this.$refs.dialogrmuser.setVisible(true, id);
        },
        doRemove(id) {
            let index = this.indexFromId(id);
            let item = this.items[index];
            const url = `${process.env.VUE_APP_BASE_URL}/v2/user/rm/${item.id}`;
            let that = this;
            axios.delete(url, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.items.splice(index, 1); // Delete the item, making vue update
                that.filterItems();
                that.$emit('snackbar', true, "Successfully removed user");
            }).catch(e => {
                console.log(e);
                that.$emit('snackbar', true, `Failed to delete: ${e.response.status}`);
            });
        },
        removeGroup(id) {
            let index = this.indexFromGId(id);
            let item = this.groups[index];
            let that = this;
            axios.delete(`${process.env.VUE_APP_BASE_URL}/v2_1/group/rm/${item.id}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.groups.splice(index, 1);
                that.filterItems();
                that.snackbar(true, "Successfully removed group");
            }).catch(e => {
                console.log(e);
                that.snackbar(true, `Failed to delete group: ${e.response.status}`);
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
                that.$emit('snackbar', true, `Failed to set ${item.username} as ${role.toLowerCase()}: ${err.response.status}`);
            });
        },
        highlightFilter(text) {
            return text.replace(new RegExp(this.filter), match => {
                return `<span class="text-highlight">${match}</span>`;
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
                return item.username.toLowerCase().includes(that.filter.toLowerCase()) || item.role.toLowerCase() === that.filter.toLowerCase();
            });
            this.filtered.sort(this.dynamicSort(this.sort));
            this.filterResults = this.filtered.length;
            that.filterGroups();
        },
        filterGroups() {
            if(this.filter === '')
                this.filteredGroups = this.groups;
            let that = this;
            this.filteredGroups = this.groups.filter(function(item) {
                return item.name.toLowerCase().includes(that.filter.toLowerCase());
            });
            this.groupResults = this.filteredGroups.length;
        },
        loadInfo() {
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2/info/system`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.systemInfo = r.data;
            }).catch(function(err) {
                console.log(err);
                that.$emit('snackbar', true, `Failed to load system info: ${err.response.status}`);
            });
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2/info/app`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.appInfo = r.data;
            }).catch(function(err) {
                console.log(err);
                that.$emit('snackbar', true, `Failed to load app info: ${err.response.status}`);
            });
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
                that.loadGroups();
            }).catch(function(error) {
                console.log(error); // API is probably unreachable
                that.loadGroups();
                that.$emit('snackbar', true, `Failed to load users: ${error.response.status}`);
            });
        },
        loadGroups() {
            let that = this;
            this.groups = []; // Why is this needed here but not in 'loadItems' ?
            let groups = this.groups;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/groups`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                console.log(`Loaded groups: ${response.data.length}`);
                response.data.map(item => {
                    groups.push(item);
                });
                that.filterItems();
                that.loading = false;
            }).catch(function(error) {
                console.log(error);
                that.filterItems();
                that.loading = false;
                that.$emit('snackbar', true, `Failed to load groups: ${error.response.status}`);
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
        },
        authChanged(login, admin) {
            this.loadItems();
            this.checkUserCreate();
            this.isAdmin = false;
            this.login = login;
            if(login === true) {
                if(admin === true) {
                    this.loadInfo();
                    this.$refs.ldap.loadProps();
                    this.isAdmin = true;
                }
                else {
                    this.systemInfo = '';
                    this.appInfo = '';
                    this.$refs.ldap.clear();
                }
            }
            else {
                this.systemInfo = '';
                this.appInfo = '';
                this.$refs.ldap.clear();
            }
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
        },
        openHome: function(event) {
            window.location.href = process.env.VUE_APP_FE_URL;
        },
        checkUserCreate() {
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/uprop/allow_local`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.allowUserCreation = r.data;
            }).catch(function(err) {
                console.log(err);
                that.allowUserCreation = true;
            });
        }
    }
};
</script>
<style scoped>
.m2-card {
    border-radius: 12px;
}
</style>
