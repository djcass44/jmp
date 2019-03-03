/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
const BASE_URL="http://localhost:7000";

const storageToken = "jmp-token";
const storageUser = "jmp-user";

// Used for triggering actions between Vue instances
const bus = new Vue();

const authCheck = new Vue({
    el: '#auth-check',
    data() {
        return {
            username: '',
            version: ''
        }
    },
    methods: {
        getAuth() {
            console.log(localStorage.getItem(storageUser));
            let username = '';
            if(localStorage.getItem(storageUser) !== null) {
                this.username = `Currently authenticated as ${localStorage.getItem(storageUser)}`;
                username = localStorage.getItem(storageUser);
            }
            else {
                this.username = "Not authenticated";
                bus.$emit('authChanged', false);
            }
            console.log(`username: ${username}`)
            const url = `${BASE_URL}/v2/verify/user/${username}`;
            axios.get(url).then(r => {
                console.log("UVALID: " + r.status);
                // bus.$emit('authChanged', true);
                return axios.get(`${BASE_URL}/v2/user`, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then((r2) => {
                    let role = r2.data;
                    console.log(`User role: ${role}`);
                    bus.$emit('authChanged', true, role === 'ADMIN');
                }).catch((err) => {
                    console.log(err);
                    bus.$emit('authChanged', true, false);
                });
            }).catch((err) => {
                console.log(err);
                console.log("User credential verification failed (this is okay if not yet authenticated)");
                this.username = "Not authenticated";
                // User verification failed, nuke local storage
                this.invalidate();
                bus.$emit('authChanged', false);
            });
        },
        invalidate() {
            localStorage.removeItem(storageUser);
            localStorage.removeItem(storageToken);
        }
    },
    created() {
        this.getAuth();
        let that = this;
        axios.get(`${BASE_URL}/v2/info`).then(r => {
            that.version = r.data;
        })
    }
});
new Vue({
    el: '#dialog-delete',
    data() {
        return {
            dialog: false,
            name: '',
            index: -1
        }
    },
    methods: {
        remove() {
            if(this.name === '')
                return;
            if(this.index <= -1)
                return;
            jumps.doRemove(this.index);
            this.dialog = false;
        }
    },
    created() {
        let that = this;
        bus.$on('dialog-delete', function(show, name, index) {
            that.dialog = show;
            that.name = name;
            that.index = index;
        });
    }
});
const jumps = new Vue({
    el: '#main-list',
    data() {
        return {
            showZero: false,
            items: []
        }
    },
    methods: {
        checkItemsLength() {
            this.showZero = this.items.length === 0;
        },
        remove(index) {
            let item = this.items[index];
            bus.$emit('dialog-delete', true, item.username, index);
        },
        doRemove(index) {
            let item = this.items[index];
            const url = `${BASE_URL}/v2/user/rm/${item.username}`;
            let that = this;
            axios.delete(url, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(r => {
                console.log(r.status);
                that.items.splice(index, 1); // Delete the item, making vue update
                that.checkItemsLength();
            }).catch(e => {
                console.log(e);
                bus.$emit('snackbar', true, `Failed to delete: ${e.response.status}`);
            });
        },
        makeAdmin(index) {
            this.setState(index, "ADMIN");
        },
        makeUser(index) {
            this.setState(index, "USER");
        },
        setState(index, role) {
            let item = this.items[index];
            let that = this;
            axios.patch(`${BASE_URL}/v2/user/permission`, `{ "username": "${item.username}", "role": "${role}", "lastRole": "${item.role}" }`, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(function(response) {
                console.log(response.status);
                Vue.set(jumps.$data.items, index, { username: item.username, role: role });
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                bus.$emit('snackbar', true, `${item.username} is now a ${role.toLowerCase()}!`);
            }).catch((err) => {
                console.log(err);
                bus.$emit('snackbar', true, `Failed to add ${item.username} as ${role.toLowerCase()}: ${err.response.status}`);
            })
        },
        loadItems() {
            let url = `${BASE_URL}/v2/users`;
            let items = this.items;
            items.length = 0; // Reset in case this is being called later (e.g. from auth)
            axios.get(url, { headers: { "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}).then(function(response) {
                console.log("Loaded items: " + response.data.length);
                response.data.map(item => {
                    items.push(item);
                });
                jumps.checkItemsLength();
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
            }).catch(function(error) {
                console.log(error); // API is probably unreachable
                jumps.checkItemsLength();
                bus.$emit('snackbar', true, `Failed to load jumps: ${e.response.status}`);
            });
        }
    },
    created() {
        this.loadItems();
    }
});
const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
const dialog_auth = new Vue({
    el: '#auth-dialog',
    data () {
        return {
            dialog: false,
            valid: false,
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.exec(v) || 'Username must not contain special characters',
                (v) => v && v.length < 37 || 'Username must be less than 37 characters'
            ],
            password: '',
            passwordRules: [
                (v) => !!v || 'This is a required field.'
            ],
            title: 'Add user',
            action: 'Create',
            create: true
        }
    },
    created () {
        const vm = this;
        bus.$on('auth-dialog', function (value) {
            vm.dialog = value;
            vm.$refs.form.reset();
        })
    },
    methods: {
        onCreate() {
            this.$refs.form.validate();
            const url = `${BASE_URL}/v2/user/add`;
            let that = this;
            let data = window.btoa(`${this.name}:${this.password}`);
            axios.put(
                url,
                {},
                {headers: { 'Authorization': 'Basic ' + data, "Content-Type": "application/json", "X-Auth-Token": localStorage.getItem(storageToken), "X-Auth-User": localStorage.getItem(storageUser)}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                bus.$emit('snackbar', true, `Created user ${that.name}`);
                jumps.loadItems();
            }).catch(e => {
                console.log(e);
                bus.$emit('snackbar', true, `Failed to create user: ${e.response.status}`);
            });
        }
    }
});
new Vue({
    el: "#create-button",
    methods: {
        openDialog: function (event) {
            if(event)
                bus.$emit('auth-dialog', true)
        }
    }
});
new Vue({
    el: "#snackbar",
    data () {
        return {
            snackbar: false,
            timeout: 6000,
            text: 'Hello, I\'m a snackbar'
        }
    },
    created () {
        const vm = this;
        bus.$on('snackbar', function (value, text) {
            vm.snackbar = value;
            vm.text = text;
        })
    },
});
