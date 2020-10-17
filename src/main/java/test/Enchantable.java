package test;

import com.knziha.plod.dictionary.Utils.BU;
import test.privateTest.BitsInputStream;

import java.io.IOException;

/** 堕落的歌者文明藏匿于字节空间之中 */
public class Enchantable {
	BitsInputStream data_in;
	static class HuffmanNode{
		int left;
		int right;
		int parent;
		int weight;
		HuffmanNode(int left, int right, int parent, int weight) {
			this.left = left;
			this.right = right;
			this.parent = parent;
			this.weight = weight;
		}
		HuffmanNode() {
			this.left = 0;
			this.right = 0;
			this.parent = -1;
			this.weight = -1;
		}
	}
	HuffmanNode[] nodes;
	int[] symidx2nodeidx;
	int next_node_position;

	Enchantable(BitsInputStream _data_in) throws IOException {
		data_in = _data_in;

		int _count = data_in.read_bits(32);
		int _bits_per_len = data_in.read_bits(8);
		int _idx_bit_size = BU.bit_length(_count);

		symidx2nodeidx = new int[_count];

		nodes = new HuffmanNode[_count];
		for (int i = 0; i < _count; i++) {
			nodes[i]= new HuffmanNode();
		}
		int root_idx = nodes.length - 1;
		next_node_position = 0;
		for (int i = 0; i < _count; i++) {
			int symidx = data_in.read_bits(_idx_bit_size);
			int length = data_in.read_bits(_bits_per_len);
			place_sym_idx(symidx, root_idx, length);
		}
	}

	public boolean place_sym_idx(int sym_idx, int node_idx, int size){
        if(size == 1){ // time to place
            if(nodes[node_idx].left == 0) {
				nodes[node_idx].left = -1 - sym_idx;
				symidx2nodeidx[sym_idx] = node_idx;
				return true;
			}
            if(nodes[node_idx].right == 0) {
				nodes[node_idx].right = -1 - sym_idx;
				symidx2nodeidx[sym_idx] = node_idx;
				return true;
			}
            return false;
		}

        if(nodes[node_idx].left == 0) {
			nodes[next_node_position] = new HuffmanNode(0, 0, node_idx, -1);
			next_node_position += 1;
			nodes[node_idx].left = next_node_position;
		}

        if(nodes[node_idx].left > 0) {
			if(place_sym_idx(sym_idx, nodes[node_idx].left - 1, size - 1))
				return true;
		}

        if(nodes[node_idx].right == 0) {
			nodes[next_node_position] = new HuffmanNode(0, 0, node_idx, -1);
			next_node_position += 1;
			nodes[node_idx].right = next_node_position;
		}

        if(nodes[node_idx].right > 0) {
			if(place_sym_idx(sym_idx, nodes[node_idx].right - 1, size - 1))
				return true;
		}

        return false;
	}

	public int decode() throws IOException {
		HuffmanNode node = nodes[nodes.length-1];
        int length = 0;
        while(true){
            length += 1;
			int bit = data_in.read_bit();
            if(bit!=0){//right
                if(node.right < 0) {//leaf
					int sym_idx = -1 - node.right;
					return sym_idx;
				}
                node = nodes[node.right - 1];
            } else {//left
                if(node.left < 0) {//leaf
					int sym_idx = -1 - node.left;
					return sym_idx;
				}
                node = nodes[node.left - 1];
			}
		}
	}
}
