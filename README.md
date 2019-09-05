# merkle-tree
Merkle Tree - A generic implementation of Merkle trees

## Usage ##

```
  // create a hash function
  UnaryOperator<String> hashFn = ...
  
  // create a concatenation function
  BinaryOperator<String, String> concatFn = (s1, s2) -> s1 + s2;
  
  // create a tree builder
  TreeBuilder<String> tbd = new TreeBuilder<>(hashFn, concatFn, "");
  
  // add some nodes and build the tree
  TreeRoot<String> root = tbd
                .collect( new TreeLeaf<>(hashFn, "s1" )
                // ....
                .collect( new TreeLeaf<>(hashFn, "s2") )
                .build();

  // get a leaf
  TreeLeaf<String> leaf1 = root.getLeaf( 0 );
  
  // validate the hash chain
  assertTrue( tbd.validate( leaf1.getChain() ) );
```

