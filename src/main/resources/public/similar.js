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

const chips = new Vue({
    el: '#main-list',
    data() {
        return {
            items: []
        }
    },
    methods: {
        open(itemName) {
            window.location.replace(`${endpoint}jump/${itemName}`)
        }
    },
    created() {
        let url = new URL(window.location.href);
        if(url.searchParams.has("query")) {
            let query = url.searchParams.get("query");
            let items = this.items;
            axios.get(`${BASE_URL}/v2/similar/${query}`).then(function (response) {
                console.log(`Loaded ${response.data.length} item(s)`);
                response.data.map(item => {
                    items.push(item);
                });
            }).catch(function (error) {
                console.log(error);
            });
        }
    }
});