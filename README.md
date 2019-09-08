# Merkle Tree

A generic implementation of Merkle trees

## Usage ##

Add *merkle-tree* as a dependency to your project.

* Gradle

```
  implementation 'ascelion.public:merkle-tree:1.0.4'
```

* Maven
````
  <dependency>
    <groupId>ascelion.public</groupId>
    <artifactId>merkle-tree</artifactId>
    <version>1.0.4</version>
  </dependency>
````

Use the TreeBuilder to create a Merkle tree

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
  TreeLeaf<String> leaf0 = root.getLeaf( 0 );
  
  // validate the hash chain
  assertTrue( tbd.validate( leaf0.getChain() ) );

```

You may also want to check the [demo project](https://github.com/ascelion/merkle-tree/tree/master/demo) for a real example - well, kind of :).

Also [see the javadoc](https://ascelion.github.io/merkle-tree/index.html).

## License ##

```
Copyright (c) 2019 ASCELION

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
