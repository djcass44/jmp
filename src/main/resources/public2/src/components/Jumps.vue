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
                <li v-for='item in filtered' :key="item.id" class="mdl-list__item mdl-list__item--two-line mdl-shadow--2dp">
                    <span class="mdl-list__item-primary-content">
                        <div v-if="item.image == null || item.image === ''">
                            <i v-if="item.personal === false" class="material-icons mdl-list__item-avatar">public</i>
                            <i v-if="item.personal === true" class="material-icons mdl-list__item-avatar">account_circle</i>
                        </div>
                        <img v-if="item.image != null && item.image !== ''" :src="item.image" class="mdl-list__item-avatar">
                        <span class="strong-title">{{ item.name }}</span>
                        <span v-html="highlight(item.location)" class="sub-text mdl-list__item-sub-title">{{ item.location }}</span>
                    </span>
                        <span class="mdl-list__item-secondary-content">
                        <!-- Right aligned menu below button -->
                        <button :id="item.id" class="mdl-button mdl-js-button mdl-button--icon">
                          <i class="material-icons">more_vert</i>
                        </button>
                    </span>
                    <ul class="mdl-menu mdl-menu--bottom-left mdl-js-menu mdl-js-ripple-effect" :for="item.id">
                        <li @click="edit(item.id)" class="mdl-menu__item">Edit</li>
                        <li @click="remove(item.id)" class="mdl-menu__item">Delete</li>
                    </ul>
                </li>
                <div v-if="showZero === true">
                    <div class="mdl-grid">
                        <div class="mdl-layout-spacer title"></div>
                        <h2 class="mdl-color-text--grey-800">Nothing to see here!</h2>
                        <div class="mdl-layout-spacer title"></div>
                    </div>
                </div>
            </ul>
            <div class="mdl-layout-spacer"></div>
        </div>
    </div>
</template>

<script>
import axios from "axios";
import { storageUser, storageToken } from "../var.js";

export default {
    data() {
        return {
            filter: '',
            filterResults: 0,
            loggedIn: false,
            showZero: false,
            items: [],
            filtered: []
        }
    },
    methods: {
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
            axios.delete(url, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(r => {
                console.log(r.status);
                that.items.splice(index, 1); // Delete the item, making vue update
                that.filterItems();
                that.checkItemsLength();
            }).catch(e => {
                console.log(e);
                this.$emit('snackbar', true, `Failed to delete: ${e.response.status}`);
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
                return item.name.match(regex);
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
            axios.get(url, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(function(response) {
                console.log("Loaded items: " + response.data.length);
                response.data.map(item => {
                    items.push(item);
                });
                that.filterItems();
                that.checkItemsLength();
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
            }).catch(function(error) {
                console.log(error); // API is probably unreachable
                that.filterItems();
                that.checkItemsLength();
                this.$emit('snackbar', true, `Failed to load jumps: ${error.response.status}`);
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
