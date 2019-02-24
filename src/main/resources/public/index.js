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
const endpoint = `${BASE_URL}/v1/`;
console.log(`endpoint: ${endpoint}`);

const storageToken = "token";
const storageID = "username";

// Used for triggering actions between Vue instances
const bus = new Vue();

const authCheck = new Vue({
    el: '#auth-check',
    data() {
        return {
            username: ''
        }
    },
    methods: {
        getAuth() {
            // console.log(localStorage.getItem("username"));
            let username = null;
            if(localStorage.getItem(storageID) !== null) {
                this.username = `Currently authenticated as ${localStorage.getItem(storageID)}`;
                username = localStorage.getItem(storageID);
            }
            else
                this.username = "Not authenticated.";
            const url = `${BASE_URL}/v2/user/${username}`;
            axios.get(url).then(r => {
                console.log("UVALID: " + r.status);
            }).catch(e => {
                console.log(e);
                this.username = "Not authenticated.";
                // User verification failed, nuke local storage
                this.invalidate();
            })
        },
        showAuth() {
            bus.$emit('auth-dialog', true);
        },
        invalidate() {
            localStorage.removeItem(storageID);
            localStorage.removeItem(storageToken);
        }
    },
    created() {
        this.getAuth();
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
            const url = endpoint + 'jumps/rm/' + item.name;
            let that = this;
            axios.delete(url).then(r => {
                console.log(r.status);
                that.items.splice(index, 1); // Delete the item, making vue update
                that.checkItemsLength();
            }).catch(e => console.log(e));
        },
        edit(index) {
            let item = this.items[index];
            bus.$emit('dialog', true, 'Edit jump point', 'Update', true, item.name, item.location, index);
        },
        highlight(text) {
            return text.replace(new RegExp("https?:\\/\\/(www\\.)?"), match => {
                if(text.startsWith("https"))
                    return `<span class="text-https">${match}</span>`;
                else
                    return `<span class="text-http">${match}</span>`;
            });
        },
        loadItems() {
            let url = endpoint + 'jumps';
            if(localStorage.getItem(storageToken) !== null)
                url += '?token=' + localStorage.getItem(storageToken);
            let items = this.items;
            items.length = 0; // Reset in case this is being called later (e.g. from auth)
            axios.get(url).then(function(response) {
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
                bus.$emit('snackbar', true, `Failed to load jumps`);
            });
        }
    },
    created() {
        this.loadItems();
    }
});
const urlRegex = new RegExp('https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)');
const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
const dialog = new Vue({
    el: '#create-dialog',
    data () {
        return {
            dialog: false,
            valid: false,
            edit: false,
            title: 'New jump point',
            action: 'Create',
            lastName: '',
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.exec(v) || 'Name must not contain special characters',
                (v) => v && v.length < 50 || 'Name must be less than 50 characters'
            ],
            location: '',
            locationRules: [
                (v) => !!v || 'This is a required field.',
                (v) => urlRegex.exec(v) || 'URL must be valid.',
                (v) => v && v.length < 2083 || 'Location must be less than 2083 characters'
            ],
            select: null,
            items: [
                "Global",
                "Personal"
            ],
            index: -1
        }
    },
    created () {
        const vm = this;
        bus.$on('dialog', function (value, title, action, edit, name, location, index) {
            if(edit)
                vm.edit = edit;
            else {
                vm.edit = false;
                vm.$refs.form.reset();
            }
            vm.dialog = value;
            vm.title = title;
            vm.action = action;
            if(name) {
                vm.name = name;
                vm.lastName = name;
            }
            if(location)
                vm.location = location;
            if(index !== undefined)
                vm.index = index;
            else
                vm.index = -1;
        })
    },
    methods: {
        update () {
            this.$refs.form.validate();
            let url = endpoint + 'jumps/edit';
            if(localStorage.getItem(storageToken) === null)
                url += '?token=' + localStorage.getItem(storageToken);
            let that = this;
            axios.patch(
                url,
                `{ "name": "${this.name}", "location": "${this.location}", "lastName": "${this.lastName}" }`,
                {headers: {"Content-Type": "application/json"}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                // jumps.$data.items = Object.assign({}, jumps.$data.items);
                console.log(that.index);
                console.log(this.index);
                Vue.set(jumps.$data.items, that.index, { name: this.name, location: this.location, personal: this.select === this.items[1] });
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                bus.$emit('snackbar', true, `Updated ${that.name}`);
            }).catch(e => {
                console.log(e);
                bus.$emit('snackbar', true, `Failed to update ${that.name}`);
            });
        },
        submit () {
            this.$refs.form.validate();
            let url = endpoint + 'jumps/add';
            const localToken = localStorage.getItem(storageToken);
            const personalJump = this.select === this.items[1];
            if(personalJump && localToken !== null)
                url += '?token=' + localToken;
            let that = this;
            axios.put(
                url,
                `{ "name": "${this.name}", "location": "${this.location}", "personal": "${personalJump}" }`,
                {headers: {"Content-Type": "application/json"}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                jumps.$data.items.push({
                    name: that.name,
                    location: that.location,
                    personal: personalJump}
                );
                jumps.checkItemsLength();
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                bus.$emit('snackbar', true, `Added ${that.name}`)
            }).catch(e => {
                console.log(e);
                bus.$emit('snackbar', true, `Failed to add ${that.name}`);
            });
        },
        clear () {
            this.name = '';
            this.location = '';
            this.$refs.form.reset();
        }
    }
});
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
            title: 'Authenticate',
            action: 'Login',
            create: false
        }
    },
    created () {
        const vm = this;
        bus.$on('auth-dialog', function (value, create) {
            vm.dialog = value;
            vm.$refs.form.reset();
            if(create) {
                vm.create = create;
                vm.title = "Add user";
                vm.action = "Create";
            }
            else {
                vm.create = false;
                vm.title = "Authenticate";
                vm.action = "Login";
            }
        })
    },
    methods: {
        onCreate() {
            this.$refs.form.validate();
            const url = `${BASE_URL}/v2/user/add`;
            let that = this;
            axios.put(
                url,
                `{ "username": "${this.name}", "password": "${this.password}" }`,
                {headers: {"Content-Type": "application/json"}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                bus.$emit('snackbar', true, `Created user ${that.name}`);
            }).catch(e => {
                console.log(e);
                bus.$emit('snackbar', true, `Failed to create user ${that.name}`);
            });
        },
        submit () {
            this.$refs.form.validate();
            const url = `${BASE_URL}/v2/user/auth`;
            let that = this;
            axios.post(
                url,
                `{ "username": "${this.name}", "password": "${this.password}" }`,
                {headers: {"Content-Type": "application/json"}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                // console.log(r.data);
                localStorage.setItem("token", r.data);
                localStorage.setItem("username", that.name);
                authCheck.getAuth();
                jumps.loadItems();
            }).catch(function(e) {
                console.log(e);
                if(e.response.status === 404)
                    bus.$emit('snackbar', true, "Password incorrect or user doesn't exist");
                else
                    bus.$emit('snackbar', true, `Failed to authenticate ${that.name}`);
            });
        }
    }
});
new Vue({
    el: "#create-button",
    methods: {
        openDialog: function (event) {
            if(event)
                bus.$emit('dialog', true, 'New jump point', 'Create')
        }
    }
});
new Vue({
    el: "#toolbar-overflow",
    methods: {
        openDialog: function (event) {
            if(event)
                bus.$emit('auth-dialog', true)
        },
        openCreateDialog: function (event) {
            if(event)
                bus.$emit('auth-dialog', true, true)
        },
        logout: function (event) {
            if(event) {
                authCheck.invalidate();
                authCheck.getAuth();
            }
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
