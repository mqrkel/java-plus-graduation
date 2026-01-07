#!/bin/bash

bash add_hit.sh ewm 	/docs 		192.168.90.199 "2025-01-01 10:00:00"
bash add_hit.sh ewm 	/docs 		192.168.90.199 "2025-02-01 10:00:00"
bash add_hit.sh ewm 	/docs 		192.168.90.199 "2025-03-01 10:00:00"
bash add_hit.sh ewm 	/docs 		192.168.90.199 "2025-03-01 10:00:00"

bash add_hit.sh ewm 	/docs 		192.168.90.200 "2025-01-02 10:00:00"

bash add_hit.sh ewm 	/docs/2  	192.168.90.199 "2025-01-02 10:00:00"
bash add_hit.sh ewm 	/docs/2  	192.168.90.199 "2025-01-02 10:00:00"

# bash add_hit.sh ewm 	/docs/2  	192.168.90.199 "2025-01-02 10:00:00"


# bash add_hit.sh ewm 	/my/path/1 	192.168.90.199 "2025-01-01 10:00:00"
# bash add_hit.sh ewm 	/my/path/2 	192.168.90.199 "2025-01-01 10:00:00"

# bash add_hit.sh ewm 	/users 		192.168.90.199 "2025-01-02 10:00:00"

# bash add_hit.sh ewm 	/users/1 	192.168.90.199 "2025-01-02 10:00:00"
# bash add_hit.sh ewm 	/users/1 	192.168.90.199 "2025-01-02 10:00:00"
# bash add_hit.sh ewm 	/users/1 	192.168.90.199 "2025-01-02 10:00:00"
# bash add_hit.sh ewm 	/users/1 	192.168.90.199 "2025-01-02 10:00:00"

# bash add_hit.sh ewm 	/users/2 	192.168.90.199 "2025-01-02 10:00:00"

