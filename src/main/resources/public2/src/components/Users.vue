<template>
    <div id="main-list" v-cloak>
        <div class="mdl-grid" v-if="filter !== ''">
            <div class="mdl-layout-spacer title"></div>
            <p>Searching for '{{ filter }}' ({{ filterResults }} results)</p>
            <div class="mdl-layout-spacer title"></div>
        </div>
        <div class="page-content mdl-grid">
            <div class="mdl-layout-spacer"></div>
            <ul class="main-list mdl-list" v-cloak>
                <li v-for='user in filtered' :key="user.id" class="mdl-list__item mdl-list__item--two-line mdl-shadow--2dp">
                    <span class="mdl-list__item-primary-content">
                        <i class="material-icons mdl-list__item-avatar">account_circle</i>
                        <span class="strong-title">{{ user.username }}</span>
                        <span class="sub-text mdl-list__item-sub-title">{{ user.role }}</span>
                    </span>
                        <span class="mdl-list__item-secondary-content">
                        <!-- Right aligned menu below button -->
                        <button :id="user.id" class="mdl-button mdl-js-button mdl-button--icon">
                          <i class="material-icons">more_vert</i>
                        </button>
                    </span>
                    <ul class="mdl-menu mdl-menu--bottom-left mdl-js-menu mdl-js-ripple-effect" :for="user.id">
                        <li v-if="user.role === 'USER'" @click="makeAdmin(user.id)" class="mdl-menu__item">Make admin</li>
                        <li v-if="user.role === 'ADMIN'" @click="makeUser(user.id)" class="mdl-menu__item">Make user</li>
                        <li @click="remove(user.id)" class="mdl-menu__item">Delete</li>
                    </ul>
                </li>
            </ul>
            <div class="mdl-layout-spacer"></div>
        </div>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageToken } from "../var.js";

export default {
    name: "Users",
    data() {
        return {
            filter: '',
            filterResults: 0,
            items: [],
            filtered: []
        }
    },
    methods: {
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
            axios.delete(url, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(r => {
                that.items.splice(index, 1); // Delete the item, making vue update
                that.filterItems();
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
            axios.patch(`${process.env.VUE_APP_BASE_URL}/v2/user/permission`, `{ "id": "${item.id}", "role": "${role}" }`, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(function(response) {
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
            axios.get(url, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(function(response) {
                console.log("Loaded items: " + response.data.length);
                response.data.map(item => {
                    items.push(item);
                });
                that.filterItems();
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
            }).catch(function(error) {
                console.log(error); // API is probably unreachable
                that.filterItems();
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
    }
};
</script>
