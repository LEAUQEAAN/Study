# Study
search [1] [ 'c1,'c2,'c3 ] { et | [ 'c1 , 0 , false ],[ 'c2 , 0 , false ],[ 'c3 , 0 , false ] | 0 } [ ['c1 < 'c2 ],[ 'c2 < 'c3 ],[ 'c3 |= 'c1 $ 1 ] ] [ ep ]  =>*  [ S2:Schedule ]{  S1:Schedules | H:Histories | N:Nat }[ RS:Relations ] [ S:Schedules ]  such that N == 2000 .
search [1] [ 'c1,'c2,'c3 | 0 ] { et | [ 'c1 , 0 , false ],[ 'c2 , 0 , false ],[ 'c3 , 0 , false ] | 0 } [ 1 ] [ ['c1 < 'c2 ],[ 'c2 < 'c3 ],[ 'c3 |= 'c1 $ 1 ] ] =>*  [ S2:Schedule | P:Nat ]{  S1:Schedules | H:Histories | N:Nat }[ M:Nat ][ RS:Relations ] such that N == N .
